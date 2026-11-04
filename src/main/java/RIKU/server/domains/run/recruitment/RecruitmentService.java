package RIKU.server.domains.run.recruitment;

import RIKU.server.domains.run.post.entity.*;
import RIKU.server.domains.run.post.repository.*;
import RIKU.server.domains.run.participant.entity.*;
import RIKU.server.domains.run.participant.repository.ParticipantRepository;
import RIKU.server.domains.run.pacer.entity.Pacer;
import RIKU.server.domains.run.pacer.repository.PacerRepository;
import RIKU.server.domains.user.entity.*;
import RIKU.server.domains.user.repository.UserRepository;
import RIKU.server.global.entity.BaseStatus;
import RIKU.server.security.AuthMember;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.*;
import static RIKU.server.domains.run.recruitment.RunEnrollment.State.*;
import java.time.*;
import java.util.*;

@Service @RequiredArgsConstructor @Transactional(readOnly=true)
public class RecruitmentService {
    private final PostRepository posts;
    private final RegularPostRepository regular;
    private final UserRepository users;
    private final ParticipantRepository participants;
    private final PacerRepository pacers;
    private final RunRecruitmentRepository recruitment;
    private final RunEnrollmentRepository enrollments;
    private final RIKU.server.infra.storage.S3Uploader storage;
    private final AttachmentRepository attachments;

    @Transactional(rollbackFor=java.io.IOException.class)
    public Long createWithImages(AuthMember actor, RecruitmentRequest request,
            org.springframework.web.multipart.MultipartFile image,
            List<org.springframework.web.multipart.MultipartFile> files) throws java.io.IOException {
        if (files != null && files.size() > 6)
            throw new ResponseStatusException(BAD_REQUEST,"코스 사진은 최대 6장까지 첨부할 수 있습니다.");
        Long id=create(actor,request);
        Post post=posts.findById(id).orElseThrow();
        if (image != null && !image.isEmpty())
            post.updatePost(post.getTitle(),post.getLocation(),post.getDate(),post.getContent(),storage.upload(image,"postImg"));
        if (files != null) for (var file:files) {
            if (!file.isEmpty()) attachments.save(Attachment.create(post,storage.upload(file,"attachmentImg")));
        }
        return id;
    }

    @Transactional
    public Long create(AuthMember actor, RecruitmentRequest request) {
        if (!request.date().isAfter(LocalDateTime.now(ZoneOffset.UTC)))
            throw new ResponseStatusException(BAD_REQUEST, "시작 시간은 현재 이후여야 합니다.");
        User creator=users.findById(actor.getId()).orElseThrow();
        if (!actor.isAdmin() && !(creator.getUserRole()==UserRole.MEMBER && Boolean.TRUE.equals(creator.getIsPacer())))
            throw new ResponseStatusException(FORBIDDEN, "운영진 또는 페이서만 정규런을 만들 수 있습니다.");
        if (request.groups().stream().map(RecruitmentRequest.Group::name).distinct().count()!=request.groups().size())
            throw new ResponseStatusException(BAD_REQUEST, "그룹 이름은 중복될 수 없습니다.");
        if (posts.findByPostCreatorIdAndTitleAndDate(creator.getId(), request.title(), request.date()).isPresent())
            throw new ResponseStatusException(CONFLICT, "이미 등록된 정규런입니다.");
        Post post=posts.save(Post.create(creator,request.title(),request.location(),request.date(),request.content(),null,PostType.REGULAR));
        regular.save(RegularPost.create(post));
        recruitment.save(new RunRecruitment(post.getId(),request.capacity(),request.groups().stream()
                .map(g->new RunRecruitment.GroupPlan(g.name(),g.pace(),g.distanceKm(),g.capacity())).toList()));
        return post.getId();
    }

    public record Entry(Long userId,String name,String role,String state,String group,int waitingNumber,String attendance) {}
    public record Summary(boolean configured,Integer capacity,List<RunRecruitment.GroupPlan> groups,
                          List<Entry> confirmed,List<Entry> waiting,Entry mine) {}
    public Summary read(Long postId,AuthMember actor) {
        activePost(postId,false);
        var config=recruitment.findById(postId);
        if(config.isEmpty()) return new Summary(false,null,List.of(),List.of(),List.of(),null);
        var attendees=participants.findWithUserByPost(posts.getReferenceById(postId));
        Map<Long,String> attendance=new HashMap<>();
        attendees.forEach(p->attendance.put(p.getUser().getId(),p.getParticipantStatus().name()));
        List<Entry> confirmed=new ArrayList<>(),waiting=new ArrayList<>();

        Entry mine=null;
        for(var e:enrollments.findByPostIdOrderByIdAsc(postId)) {
            int rank=e.getState()==WAITLISTED?waiting.size()+1:0;
            var entry=new Entry(e.getUser().getId(),e.getUser().getName(),e.getRole().name(),e.getState().name(),e.getGroup(),rank,
                    attendance.getOrDefault(e.getUser().getId(),"WAITLISTED"));
            (e.getState()==CONFIRMED?confirmed:waiting).add(entry);
            if(e.getUser().getId().equals(actor.getId())) mine=entry;
        }
        var c=config.get();
        return new Summary(true,c.getCapacity(),List.copyOf(c.getGroups()),confirmed,waiting,mine);
    }

    @Transactional
    public Summary join(Long postId,AuthMember actor,RunEnrollment.Role role,String group) {
        Post post=activePost(postId,true); writable(post);
        var config=recruitment.findById(postId).orElseThrow(()->new ResponseStatusException(CONFLICT,"모집 설정이 없는 기존 런입니다."));
        plan(config,group);
        User user=users.findById(actor.getId()).orElseThrow();
        if(role==RunEnrollment.Role.PACER && !Boolean.TRUE.equals(user.getIsPacer()))
            throw new ResponseStatusException(FORBIDDEN,"페이서 권한이 필요합니다.");
        var existing=enrollments.findByPostIdAndUserId(postId,actor.getId());
        if(existing.isPresent()) {
            var e=existing.get();
            if(e.getRole()!=role) throw new ResponseStatusException(CONFLICT,"역할을 바꾸려면 기존 신청을 취소한 뒤 다시 신청해주세요.");
            var participant=participants.findByPostAndUser(post,user);
            if(participant.isPresent() && participant.get().getParticipantStatus()==ParticipantStatus.ATTENDED)
                throw new ResponseStatusException(CONFLICT,"출석 후에는 그룹을 변경할 수 없습니다.");
            if (group.equals(e.getGroup())) return read(postId, actor);
            if (e.getState() == WAITLISTED)
                throw new ResponseStatusException(CONFLICT, "대기 취소 후 새 그룹에 신청해주세요.");
            if (!seats(config).groupAvailable(group))
                throw new ResponseStatusException(CONFLICT, "이동할 그룹의 정원이 찼습니다.");
            e.changeGroup(group);
            participant.ifPresent(p->p.updateGroup(group));
            if(e.getState()==CONFIRMED && role==RunEnrollment.Role.PACER) {
                pacers.deleteByUserAndPost(user,post); pacers.flush(); addPacer(post,e,config);
            }
            promote(post, config);
            return read(postId,actor);
        }
        var entry=enrollments.save(new RunEnrollment(postId,user,role,group,seats(config).available(group) ? CONFIRMED : WAITLISTED));
        if(entry.getState()==CONFIRMED) addParticipant(post,entry,config);
        return read(postId,actor);
    }

    @Transactional
    public Summary cancel(Long postId,AuthMember actor) {
        Post post=activePost(postId,true); writable(post);
        var entry=enrollments.findByPostIdAndUserId(postId,actor.getId());
        if(entry.isEmpty()) return read(postId,actor);
        var e=entry.get();
        var participant=participants.findByPostAndUser(post,e.getUser());
        if(participant.isPresent() && participant.get().getParticipantStatus()==ParticipantStatus.ATTENDED)
            throw new ResponseStatusException(CONFLICT,"출석 완료 후에는 신청을 취소할 수 없습니다.");
        participant.ifPresent(participants::delete);
        if(e.getRole()==RunEnrollment.Role.PACER) pacers.deleteByUserAndPost(e.getUser(),post);
        enrollments.delete(e); enrollments.flush();
        if (e.getState() == CONFIRMED) promote(post, recruitment.findById(postId).orElseThrow());
        return read(postId,actor);
    }

    // PostService holds the post write lock for the whole edit transaction.
    @Transactional
    public void changeCapacity(Post post, Integer capacity) {
        edit(post, true, capacity, null);
    }

    public void edit(Post post, boolean changeTotal, Integer capacity, List<RunRecruitment.GroupPlan> plans) {
        writable(post);
        var config = recruitment.findById(post.getId()).orElseGet(() -> {
            if (plans == null || plans.isEmpty()) throw new ResponseStatusException(CONFLICT, "그룹을 설정해주세요.");
            var imported = recruitment.save(new RunRecruitment(post.getId(), null, List.of()));
            var leaders = new HashSet<>(participants.findPacerUserIdsByPost(post));
            for (var p : participants.findWithUserByPost(post)) {
                if (p.getGroup() == null || p.getGroup().isBlank())
                    throw new ResponseStatusException(CONFLICT, "기존 참가자의 그룹을 먼저 지정해주세요.");
                enrollments.save(new RunEnrollment(post.getId(), p.getUser(), leaders.contains(p.getUser().getId())
                        ? RunEnrollment.Role.PACER : RunEnrollment.Role.RUNNER, p.getGroup(), CONFIRMED));
            }
            return imported;
        });
        if (plans != null) {
            if (plans.isEmpty()) throw new ResponseStatusException(BAD_REQUEST, "그룹이 하나 이상 필요합니다.");
            var entries = enrollments.findByPostIdOrderByIdAsc(post.getId());
            config.replacePlans(plans, entries.stream().map(RunEnrollment::getGroup).toList());
            for (var pacer : pacers.findByPost(post)) {
                var p = plan(config, pacer.getGroup());
                pacer.updateDetails(p.getName(), p.getPace(), p.getDistanceKm().toPlainString());
            }
        }
        if (changeTotal) config.changeCapacity(capacity);
        promote(post, config);
    }

    private GroupSeats seats(RunRecruitment config) {
        var confirmed = enrollments.findByPostIdOrderByIdAsc(config.getPostId()).stream()
                .filter(e -> e.getState() == CONFIRMED).map(RunEnrollment::getGroup).toList();
        return new GroupSeats(config, confirmed);
    }

    private void promote(Post post, RunRecruitment config) {
        var seats = seats(config);
        seats.validate();
        for (var entry : enrollments.findByPostIdOrderByIdAsc(post.getId())) {
            if (entry.getState() == WAITLISTED && seats.available(entry.getGroup())) {
                entry.confirm();
                addParticipant(post, entry, config);
                seats.take(entry.getGroup());
            }
        }
    }

    private void addParticipant(Post post,RunEnrollment e,RunRecruitment config) {
        participants.save(Participant.createWithGroup(post,e.getUser(),e.getGroup()));
        if(e.getRole()==RunEnrollment.Role.PACER) addPacer(post,e,config);
    }
    private void addPacer(Post post,RunEnrollment e,RunRecruitment config) {
        var group=plan(config,e.getGroup());
        pacers.save(Pacer.create(e.getUser(),post,e.getGroup(),group.getPace(),group.getDistanceKm().toPlainString()));
    }
    private RunRecruitment.GroupPlan plan(RunRecruitment c,String group) {
        return c.getGroups().stream().filter(g->g.getName().equals(group)).findFirst()
                .orElseThrow(()->new ResponseStatusException(BAD_REQUEST,"존재하지 않는 그룹입니다."));
    }
    private Post activePost(Long id,boolean lock) {
        Post post=(lock?posts.findForUpdate(id):posts.findById(id)).orElseThrow(()->new ResponseStatusException(NOT_FOUND,"러닝을 찾을 수 없습니다."));
        if(post.getPostType()!=PostType.REGULAR||post.getStatus()!=BaseStatus.ACTIVE)
            throw new ResponseStatusException(NOT_FOUND,"정규런을 찾을 수 없습니다.");
        return post;
    }
    private void writable(Post post) {
        if(post.getPostStatus()!=PostStatus.NOW || post.getDate().isBefore(LocalDateTime.now(ZoneOffset.UTC)))
            throw new ResponseStatusException(CONFLICT,"모집이 종료된 러닝입니다.");
    }
}
