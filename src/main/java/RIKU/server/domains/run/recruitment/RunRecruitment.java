package RIKU.server.domains.run.recruitment;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.*;

@Entity @Getter @NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RunRecruitment {
    @Id private Long postId;
    private Integer capacity;
    @ElementCollection
    @CollectionTable(name = "run_recruitment_group", joinColumns = @JoinColumn(name = "post_id"))
    @OrderColumn(name = "group_order")
    private List<GroupPlan> groups = new ArrayList<>();

    public RunRecruitment(Long postId, Integer capacity, List<GroupPlan> groups) {
        this.postId = postId;
        this.capacity = capacity;
        this.groups.addAll(groups);
    }

    public void changeCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public void replacePlans(List<GroupPlan> plans, Collection<String> occupied) {
        var names = new HashSet<String>();
        for (var p : plans) {
            if (p == null || p.name == null || p.name.isBlank() || p.name.length() > 20 ||
                    !names.add(p.name) || p.pace == null || !p.pace.matches("[0-9]{1,2}:[0-5][0-9]") ||
                    p.distanceKm == null || p.distanceKm.compareTo(new BigDecimal("0.1")) < 0 ||
                    p.distanceKm.compareTo(BigDecimal.valueOf(200)) > 0 || plans.size() > 20 ||
                    (p.capacity != null && (p.capacity < 1 || p.capacity > 1000)))
                throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "그룹 설정을 확인해주세요.");
        }
        for (String group : occupied)
            if (group != null && !group.isBlank() && !names.contains(group))
                throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT, group + "그룹에 신청자가 있습니다.");
        groups.clear();
        groups.addAll(plans);
    }

    @Embeddable @Getter @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor
    public static class GroupPlan {
        @Column(name = "group_name") private String name;
        private String pace;
        @Column(precision = 7, scale = 2) private BigDecimal distanceKm;
        @Column(name = "group_capacity") private Integer capacity;
        public GroupPlan(String name, String pace, BigDecimal distanceKm) { this(name, pace, distanceKm, null); }
    }
}
