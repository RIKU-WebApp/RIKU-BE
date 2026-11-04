package RIKU.server.domains.point.ranking;

import RIKU.server.domains.point.entity.PointType;
import RIKU.server.domains.point.entity.UserPoint;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/** Copy persisted point coordinates inside the write transaction, including rows being deleted. */
public record RankingChanged(boolean membershipChanged, Set<PointChange> points) {
    public RankingChanged { points = Set.copyOf(points); }

    public static RankingChanged members() { return new RankingChanged(true, Set.of()); }

    public static RankingChanged points(Collection<UserPoint> points) {
        return new RankingChanged(false, points.stream()
                .map(point -> new PointChange(point.getCreatedAt(), point.getPointType()))
                .collect(Collectors.toSet()));
    }

    public boolean affects(RankingQuery query) {
        return membershipChanged || points.stream().anyMatch(point -> query.includes(point.createdAt(), point.pointType()));
    }

    public record PointChange(LocalDateTime createdAt, PointType pointType) {}
}
