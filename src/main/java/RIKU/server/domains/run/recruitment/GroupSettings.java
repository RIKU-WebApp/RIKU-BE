package RIKU.server.domains.run.recruitment;

import java.math.BigDecimal;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/** Multipart forms omit replaceGroups to preserve the existing plans. */
@Getter @Setter
public class GroupSettings {
    private boolean replaceGroups;
    private List<Row> groupPlans = new java.util.ArrayList<>();
    private boolean replacePacers;

    @Getter @Setter
    public static class Row {
        private String name;
        private String pace;
        private BigDecimal distanceKm;
        private Integer capacity;
    }
    public List<RunRecruitment.GroupPlan> plans() {
        return groupPlans.stream().map(row -> new RunRecruitment.GroupPlan(
                row.name, row.pace, row.distanceKm, row.capacity)).toList();
    }
}
