package RIKU.server.domains.point.ranking;

import RIKU.server.domains.point.entity.PointType;
import java.time.LocalDateTime;
import java.util.Set;

/** Null endExclusive identifies the open-ended season; event type order is irrelevant. */
public record RankingQuery(LocalDateTime start, LocalDateTime endExclusive, Set<PointType> pointTypes) {
    public RankingQuery {
        pointTypes = Set.copyOf(pointTypes);
    }

    public static RankingQuery season(LocalDateTime start) {
        return new RankingQuery(start, null, Set.of());
    }

    public boolean includes(LocalDateTime createdAt, PointType type) {
        // Match the aggregate SQL, including its treatment of historical null timestamps/types.
        return createdAt != null && !createdAt.isBefore(start)
                && (endExclusive == null || (createdAt.isBefore(endExclusive) && type != null && pointTypes.contains(type)));
    }
}
