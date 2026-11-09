package RIKU.server.domains.community;

import RIKU.server.domains.user.repository.UserRepository;
import RIKU.server.domains.run.participant.repository.ParticipantRepository;
import RIKU.server.domains.run.participant.entity.ParticipantStatus;
import RIKU.server.domains.run.post.entity.PostStatus;
import RIKU.server.domains.run.recruitment.RunRecruitmentRepository;
import RIKU.server.global.entity.BaseStatus;
import RIKU.server.security.AuthMember;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.data.domain.*;
import static org.springframework.http.HttpStatus.*;
import java.math.BigDecimal;
import java.util.*;

@Service @RequiredArgsConstructor @Transactional(readOnly=true)
public class CommunityService {
    private final UserRepository users;
    private final ParticipantRepository participants;
    private final RunRecruitmentRepository recruitment;
    private final PersonalRunRecordRepository records;
    private final BadgeAwardRepository awards;
    private final FeedbackRepository feedback;
    private final RankHonors honors;
    private final AutomaticBadges automatic;
    private final BadgeStorage storage;
    public record Badge(String code,String name,String description,String source) {}
    public record PublicProfile(String name,List<Badge> badges,List<RankHonors.Medal> medals) {}
    public record MyActivity(BigDecimal verifiedKm,BigDecimal personalKm,List<Badge> badges,List<PersonalRunRecord> records) {}
    public record BadgePreview(List<Badge> badges, List<RankHonors.Medal> medals, int medalCount) {}

    public BadgePreview badgePreview(long id) {
        var badges = automatic.previewGrants(id, 3);
        if (badges.size() < 3) {
            awards.findByUserIdOrderByIdAsc(id, PageRequest.of(0, 3 - badges.size()))
                    .forEach(award -> badges.add(manualBadge(award)));
        }
        var ranks = honors.previewForMember(id);
        return new BadgePreview(badges, ranks.medals(), ranks.total());
    }

    public PublicProfile profile(Long id) {
        var user=users.findById(id).filter(u->u.getStatus()==BaseStatus.ACTIVE)
                .orElseThrow(()->new ResponseStatusException(NOT_FOUND,"회원을 찾을 수 없습니다."));
        return new PublicProfile(user.getName(),automatic.grants(id), honors.issuedForMember(id));
    }
    public MyActivity privateActivity(Long id) {
        if (storage.installed()) return activity(id,true);
        // Without a ledger the legacy distance is progress, never proof of issuance.
        var legacy=activity(id,false);
        return new MyActivity(legacy.verifiedKm(),legacy.personalKm(),
                legacy.badges().stream().filter(b -> b.source().equals("ADMIN")).toList(),legacy.records());
    }
    public MyActivity activity(Long id) {
        return activity(id,false);
    }
    private MyActivity activity(Long id,boolean ledger) {
        var personal=records.findByUserIdOrderByDateDescIdDesc(id);
        BigDecimal personalKm=personal.stream().map(PersonalRunRecord::getDistanceKm).reduce(BigDecimal.ZERO,BigDecimal::add);
        if(ledger) {
            var badges=new ArrayList<>(automatic.grants(id));
            addManualAwards(id,badges);
            return new MyActivity(automatic.verifiedKm(id),personalKm,badges,personal);
        }
        BigDecimal verified=BigDecimal.ZERO;
        var attended=participants.findAttendedForBadges(id,ParticipantStatus.ATTENDED,PostStatus.CLOSED,BaseStatus.ACTIVE);
        var configs=attended.isEmpty() ? List.<RIKU.server.domains.run.recruitment.RunRecruitment>of()
                : recruitment.findWithGroupsByPostIds(attended.stream().map(p->p.getPost().getId()).distinct().toList());
        var byPost=new HashMap<Long,RIKU.server.domains.run.recruitment.RunRecruitment>();
        configs.forEach(c->byPost.put(c.getPostId(),c));
        for(var p:attended) {
            var c=byPost.get(p.getPost().getId());
            if(c!=null) verified=verified.add(c.getGroups().stream().filter(g->g.getName().equals(p.getGroup()))
                    .map(RIKU.server.domains.run.recruitment.RunRecruitment.GroupPlan::getDistanceKm).findFirst().orElse(BigDecimal.ZERO));
        }
        List<Badge> badges=new ArrayList<>();
        for(int km:new int[]{10,50,100,300,500,1000}) {
            if(verified.compareTo(BigDecimal.valueOf(km))>=0)
                badges.add(new Badge("CLUB_"+km,km+"km 함께 달리기","출석이 확인된 공식 러닝 누적 "+km+"km","CLUB"));
        }
        addManualAwards(id,badges);
        return new MyActivity(verified,personalKm,badges,personal);
    }
    private void addManualAwards(Long id,List<Badge> badges) {
        awards.findByUserIdOrderByIdAsc(id).forEach(a->badges.add(manualBadge(a)));
    }
    private static Badge manualBadge(BadgeAward award) {
        return new Badge("AWARD_"+award.getId(),award.getName(),award.getDescription(),"ADMIN");
    }
    @Transactional public PersonalRunRecord record(AuthMember actor,CommunityController.RecordRequest r) {
        storage.lockRecordOwner(actor.getId());
        return records.save(new PersonalRunRecord(actor.getId(),r.date(),r.distanceKm(),r.minutes(),r.title().trim()));
    }
    @Transactional public void deleteRecord(AuthMember actor,Long id) {
        storage.lockRecordOwner(actor.getId());
        var r=records.findById(id).filter(x->x.getUserId().equals(actor.getId()))
                .orElseThrow(()->new ResponseStatusException(NOT_FOUND,"내 기록을 찾을 수 없습니다."));
        records.delete(r);
    }
    @Transactional public void award(AuthMember actor,Long id,CommunityController.AwardRequest r) {
        admin(actor);
        if(!users.existsById(id)) throw new ResponseStatusException(NOT_FOUND,"회원을 찾을 수 없습니다.");
        if(awards.existsByUserIdAndName(id,r.name().trim())) throw new ResponseStatusException(CONFLICT,"이미 수여한 배지입니다.");
        awards.save(new BadgeAward(id,r.name().trim(),r.description(),actor.getId()));
    }
    @Transactional public Feedback submit(AuthMember actor,CommunityController.FeedbackRequest r) {
        return feedback.save(new Feedback(actor.getId(),r.category(),r.title().trim(),r.content().trim()));
    }
    public Page<Feedback> myFeedback(AuthMember actor,int page) {
        return feedback.findByUserIdOrderByIdDesc(actor.getId(),PageRequest.of(Math.max(0,page),20));
    }
    public Page<Feedback> adminFeedback(AuthMember actor,int page) {
        admin(actor); return feedback.findAll(PageRequest.of(Math.max(0,page),20,Sort.by(Sort.Direction.DESC,"id")));
    }
    @Transactional public Feedback respond(AuthMember actor,Long id,CommunityController.AnswerRequest r) {
        admin(actor); var f=feedback.findById(id).orElseThrow(()->new ResponseStatusException(NOT_FOUND,"건의사항을 찾을 수 없습니다."));
        f.respond(r.status(),r.answer()); return f;
    }
    private void admin(AuthMember actor) { if(!actor.isAdmin()) throw new ResponseStatusException(FORBIDDEN,"관리자 권한이 필요합니다."); }
}
