package RIKU.server.domains.run.recruitment;

import RIKU.server.domains.run.participant.entity.Participant;
import RIKU.server.domains.run.participant.repository.ParticipantRepository;
import RIKU.server.domains.run.post.entity.Post;
import RIKU.server.domains.run.post.entity.PostType;
import RIKU.server.domains.run.post.repository.PostRepository;
import RIKU.server.domains.user.entity.User;
import RIKU.server.global.entity.BaseStatus;
import java.util.List;
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

    public record View(Integer capacity, List<String> waiting, boolean mine) {}

    private Post checked(Post post, String type) {
        if (post.getStatus() != BaseStatus.ACTIVE || post.getPostType() == PostType.REGULAR
                || !post.getPostType().name().equalsIgnoreCase(type)) throw new ResponseStatusException(NOT_FOUND);
        return post;
    }

    @Transactional(readOnly = true)
    public View read(String type, Long id, Long member) {
        checked(posts.findById(id).orElseThrow(() -> new ResponseStatusException(NOT_FOUND)), type);
        var entries = queue.findByPostIdOrderByIdAsc(id);
        return new View(limits.findById(id).map(RunRecruitment::getCapacity).orElse(null),
                entries.stream().map(e -> e.getUser().getName()).toList(),
                entries.stream().anyMatch(e -> e.getUser().getId().equals(member)));
    }

    @Transactional
    public void leave(String type, Long id, Long member) {
        checked(posts.findForUpdate(id).orElseThrow(() -> new ResponseStatusException(NOT_FOUND)), type);
        queue.findByPostIdAndUserId(id, member).ifPresent(queue::delete);
    }

    // Creation and all later mutations run inside the caller's transaction and post write lock.
    public void configure(Post post, Integer capacity) {
        var config = limits.findById(post.getId());
        if (capacity == null && config.isEmpty()) return;
        if (capacity != null && (capacity < 1 || capacity > 1000))
            throw new ResponseStatusException(BAD_REQUEST, "정원은 1~1000명으로 입력해주세요.");
        var limit = config.orElseGet(() -> new RunRecruitment(post.getId(), null, List.of()));
        limit.changeCapacity(capacity);
        limits.save(limit);
        fillVacancies(post);
    }

    public boolean enqueueIfFull(Post post, User user, String group) {
        var capacity = limits.findById(post.getId()).map(RunRecruitment::getCapacity);
        if (capacity.isEmpty() || participants.countByPostId(post.getId()) < capacity.get()) return false;
        if (queue.findByPostIdAndUserId(post.getId(), user.getId()).isEmpty()) {
            queue.save(new RunEnrollment(post.getId(), user, RunEnrollment.Role.RUNNER,
                    group == null ? "" : group, WAITLISTED));
        }
        return true;
    }

    public void fillVacancies(Post post) {
        var limit = limits.findById(post.getId());
        if (limit.isEmpty()) return;
        Integer capacity = limit.get().getCapacity();
        int confirmed = participants.countByPostId(post.getId());
        if (capacity != null && confirmed > capacity)
            throw new ResponseStatusException(CONFLICT, "확정 인원보다 정원을 줄일 수 없습니다.");
        for (var entry : queue.findByPostIdOrderByIdAsc(post.getId())) {
            if (capacity != null && confirmed >= capacity) break;
            participants.save(post.getPostType() == PostType.TRAINING
                    ? Participant.createWithGroup(post, entry.getUser(), entry.getGroup())
                    : Participant.create(post, entry.getUser()));
            queue.delete(entry);
            confirmed++;
        }
    }

    public void checkPacer(Post post, Long member) {
        if (queue.findByPostIdAndUserId(post.getId(), member).isPresent())
            throw new ResponseStatusException(CONFLICT, "대기 중인 회원은 페이서로 지정할 수 없습니다.");
    }
}
