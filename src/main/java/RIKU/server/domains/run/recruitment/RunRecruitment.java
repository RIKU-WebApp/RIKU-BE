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

    @Embeddable @Getter @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor
    public static class GroupPlan {
        @Column(name = "group_name") private String name;
        private String pace;
        @Column(precision = 7, scale = 2) private BigDecimal distanceKm;
    }
}
