package RIKU.server.domains.run.recruitment;

import RIKU.server.domains.run.participant.entity.Participant;
import RIKU.server.domains.run.participant.repository.ParticipantRepository;
import RIKU.server.domains.run.post.entity.Post;
import RIKU.server.domains.run.post.entity.PostType;
import RIKU.server.domains.run.post.repository.PostRepository;
import RIKU.server.domains.user.entity.User;
import RIKU.server.global.entity.BaseStatus;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.*;
import static RIKU.server.domains.run.recruitment.RunEnrollment.State.WAITLISTED;

@Service
@RequiredArgsConstructor
public class RunWaitlist {
    private final PostRepository posts;
    private final ParticipantRepository participants;
    private final RunRecruitmentRepository limits;
    private final RunEnrollmentRepository queue;

    public record View(Integer capacity, List<String> waiting, boolean mine,
                       List<RunRecruitment.GroupPlan> groups, Map<String, Long> usedByGroup) {}

    private Post checked(Post post, String type) {
        if (post.getStatus() != BaseStatus.ACTIVE || post.getPostType() == PostType.REGULAR
                || !post.getPostType().name().equalsIgnoreCase(type)) throw new ResponseStatusException(NOT_FOUND);
        return post;
    }

    @Transactional(readOnly = true)
    public View read(String type, Long id, Long member) {
        checked(posts.findById(id).orElseThrow(() -> new ResponseStatusException(NOT_FOUND)), type);
        var entries = queue.findRoster(id);
        var config = limits.findOneByPostId(id);
        Map<String, Long> used = new HashMap<>();
        if (config.isPresent()) {
            participants.countConfirmedByGroup(id).forEach(row -> used.put(row.getName(), row.getUsed()));
        }
        return new View(config.map(RunRecruitment::getCapacity).orElse(null),
                entries.stream().map(RunEnrollmentRepository.RosterRow::getName).toList(),
                entries.stream().anyMatch(e -> e.getUserId().equals(member)),
                config.map(c -> List.copyOf(c.getGroups())).orElse(List.of()), used);
    }

    @Transactional
    public void leave(String type, Long id, Long member) {
        checked(posts.findForUpdate(id).orElseThrow(() -> new ResponseStatusException(NOT_FOUND)), type);
        queue.findByPostIdAndUserId(id, member).ifPresent(queue::delete);
    }

    // Creation and all later mutations run inside the caller's transaction and post write lock.
    public void configure(Post post, Integer capacity) {
        configure(post, true, capacity, null);
    }

    public void configure(Post post, boolean changeTotal, Integer capacity, List<RunRecruitment.GroupPlan> plans) {
        var config = limits.findById(post.getId());
        if (post.getPostType() == PostType.TRAINING && plans != null && plans.isEmpty())
            throw new ResponseStatusException(BAD_REQUEST, "훈련 그룹을 하나 이상 남겨주세요.");
        if (capacity == null && (plans == null || plans.isEmpty()) && config.isEmpty()) return;
        if (capacity != null && (capacity < 1 || capacity > 1000))
            throw new ResponseStatusException(BAD_REQUEST, "정원은 1~1000명으로 입력해주세요.");
        var limit = config.orElseGet(() -> new RunRecruitment(post.getId(), null, List.of()));
        if (plans != null) {
            var occupied = new java.util.ArrayList<String>();
            participants.findByPost(post).forEach(p -> occupied.add(p.getGroup()));
            for (var entry : queue.findByPostIdOrderByIdAsc(post.getId())) {
                if (!plans.isEmpty() && entry.getGroup().isBlank())
                    throw new ResponseStatusException(CONFLICT, "그룹 미지정 대기자가 있어 그룹을 설정할 수 없습니다.");
                occupied.add(entry.getGroup());
            }
            limit.replacePlans(plans, occupied);
        }
        if (changeTotal) limit.changeCapacity(capacity);
        limits.save(limit);
        fillVacancies(post);
    }

    public boolean enqueueIfFull(Post post, User user, String group) {
        var limit = limits.findById(post.getId());
        validateGroup(post, limit.orElse(null), group);
        if (limit.isEmpty()) return false;
        String requested = group == null ? "" : group;
        var existing = queue.findByPostIdAndUserId(post.getId(), user.getId());
        if (existing.isPresent()) {
            if (!requested.equals(existing.get().getGroup()))
                throw new ResponseStatusException(CONFLICT, "대기 취소 후 새 그룹에 신청해주세요.");
            return true;
        }
        if (seats(post, limit.get()).available(group)) return false;
        queue.save(new RunEnrollment(post.getId(), user, RunEnrollment.Role.RUNNER, requested, WAITLISTED));
        return true;
    }

    public void fillVacancies(Post post) {
        var limit = limits.findById(post.getId());
        if (limit.isEmpty()) return;
        seats(post, limit.get()).promote(queue, null, entry -> {
            participants.save(entry.getGroup().isBlank() ? Participant.create(post, entry.getUser())
                    : Participant.createWithGroup(post, entry.getUser(), entry.getGroup()));
            queue.delete(entry);
        });
    }

    private GroupSeats seats(Post post, RunRecruitment limit) {
        return new GroupSeats(limit, participants.findOccupiedGroups(post));
    }

    private void validateGroup(Post post, RunRecruitment limit, String group) {
        if (limit == null || limit.getGroups().isEmpty()) {
            if (post.getPostType() != PostType.TRAINING && group != null && !group.isBlank())
                throw new ResponseStatusException(BAD_REQUEST, "설정되지 않은 그룹입니다.");
            return;
        }
        if ( limit.getGroups().stream().noneMatch(p -> p.getName().equals(group)))
            throw new ResponseStatusException(BAD_REQUEST, "신청할 그룹을 선택해주세요.");
    }

    public void move(Post post, Participant participant, String group) {
        var limit = limits.findById(post.getId());
        validateGroup(post, limit.orElse(null), group);
        if (limit.isPresent()) {
            if (!group.equals(participant.getGroup()) && !seats(post, limit.get()).groupAvailable(group))
                throw new ResponseStatusException(CONFLICT, "이동할 그룹의 정원이 찼습니다.");
        }
        participant.updateGroup(group);
        fillVacancies(post);
    }

    public boolean hasGroups(Post post) {
        return limits.findById(post.getId()).filter(c -> !c.getGroups().isEmpty()).isPresent();
    }

    public void checkQueuedPacers(Long postId, List<Long> members) {
        for (int i = 0; i < members.size(); i += 500) {
            if (!queue.findQueuedMemberIds(postId, members.subList(i, Math.min(i + 500, members.size()))).isEmpty())
                throw new ResponseStatusException(CONFLICT, "대기 중인 회원은 페이서로 지정할 수 없습니다.");
        }
    }
}
