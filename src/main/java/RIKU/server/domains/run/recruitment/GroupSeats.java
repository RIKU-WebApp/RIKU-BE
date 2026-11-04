package RIKU.server.domains.run.recruitment;

import java.util.*;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.*;

/** Counts confirmed runners and pacers together under the parent post lock. */
final class GroupSeats {
    private final RunRecruitment config;
    private final Map<String,Integer> used = new HashMap<>();
    private int total;

    GroupSeats(RunRecruitment config, List<String> confirmed) {
        this.config = config;
        confirmed.forEach(this::take);
    }
    void take(String group) { total++; used.merge(group, 1, Integer::sum); }
    boolean groupAvailable(String group) {
        return config.getGroups().stream().filter(p -> p.getName().equals(group))
                .allMatch(p -> p.getCapacity() == null || used.getOrDefault(group, 0) < p.getCapacity());
    }
    boolean available(String group) {
        return (config.getCapacity() == null || total < config.getCapacity()) && groupAvailable(group);
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
