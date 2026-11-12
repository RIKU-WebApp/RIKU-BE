package RIKU.server.domains.run.recruitment;

import java.util.*;
import java.util.function.Consumer;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.*;

/** Counts confirmed runners and pacers together under the parent post lock. */
final class GroupSeats {
    private static final int PAGE_SIZE = 64;
    private final RunRecruitment config;
    private final Map<String,Integer> used = new HashMap<>();
    private int total;

    GroupSeats(RunRecruitment config, List<GroupOccupancy> confirmed) {
        this.config = config;
        for (var group : confirmed) {
            int count = Math.toIntExact(group.getUsed());
            used.put(group.getName(), count);
            total += count;
        }
    }
    void take(String group) { total++; used.merge(group, 1, Integer::sum); }
    boolean groupAvailable(String group) {
        return config.getGroups().stream().filter(p -> p.getName().equals(group))
                .allMatch(p -> p.getCapacity() == null || used.getOrDefault(group, 0) < p.getCapacity());
    }
    boolean available(String group) {
        return (config.getCapacity() == null || total < config.getCapacity()) && groupAvailable(group);
    }
    // Caller holds the parent post lock. Scan lightweight rows in FIFO order, then load only winners.
    void promote(RunEnrollmentRepository queue, RunEnrollment.State state, Consumer<RunEnrollment> promote) {
        validate();
        if (config.getGroups().stream().allMatch(group -> group.getCapacity() == null)) {
            promoteWithoutGroupLimits(queue, state, promote);
            return;
        }
        Long after = null;
        var page = PageRequest.of(0, PAGE_SIZE);
        while (config.getCapacity() == null || total < config.getCapacity()) {
            var candidates = queue.candidates(config.getPostId(), state, after, page);
            var selected = new ArrayList<Long>();
            for (var candidate : candidates) {
                if (available(candidate.getGroupName())) {
                    selected.add(candidate.getId());
                    take(candidate.getGroupName());
                }
            }
            if (!selected.isEmpty()) queue.findByIdInOrderByIdAsc(selected).forEach(promote);
            if (candidates.size() < page.getPageSize()) break;
            after = candidates.get(candidates.size() - 1).getId();
        }
    }

    private void promoteWithoutGroupLimits(RunEnrollmentRepository queue, RunEnrollment.State state,
                                          Consumer<RunEnrollment> promote) {
        Long after = null;
        while (config.getCapacity() == null || total < config.getCapacity()) {
            int limit = config.getCapacity() == null ? PAGE_SIZE : Math.min(PAGE_SIZE, config.getCapacity() - total);
            var entries = queue.nextEntries(config.getPostId(), state, after, PageRequest.of(0, limit));
            for (var entry : entries) {
                promote.accept(entry);
                take(entry.getGroup());
            }
            if (entries.size() < limit) break;
            after = entries.get(entries.size() - 1).getId();
        }
    }

    void validate() {
        if (!config.getGroups().isEmpty() && used.keySet().stream().anyMatch(group -> group != null && !group.isBlank()
                && config.getGroups().stream().noneMatch(p -> p.getName().equals(group))))
            throw new ResponseStatusException(CONFLICT, "참가자가 설정되지 않은 그룹에 있습니다.");
        if (config.getCapacity() != null && total > config.getCapacity())
            throw new ResponseStatusException(CONFLICT, "총정원은 확정 인원보다 작을 수 없습니다.");
        for (var plan : config.getGroups())
            if (plan.getCapacity() != null && used.getOrDefault(plan.getName(), 0) > plan.getCapacity())
                throw new ResponseStatusException(CONFLICT, plan.getName() + "그룹 정원이 확정 인원보다 작습니다.");
    }
}
