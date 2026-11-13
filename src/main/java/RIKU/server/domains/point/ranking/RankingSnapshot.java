package RIKU.server.domains.point.ranking;

import RIKU.server.domains.point.dto.response.ReadUserPointResponse;
import RIKU.server.domains.point.dto.response.ReadUserRankingResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** No managed entities or caller-specific response is stored in the shared snapshot. */
public final class RankingSnapshot {
    private final List<ReadUserPointResponse> top20;
    private final Map<Long, ReadUserPointResponse> members;
    private final Map<Long, Integer> ranks;
    private final Map<Long, Integer> activityRanks;

    public RankingSnapshot(List<ReadUserPointResponse> sortedMembers) {
        this(sortedMembers, Set.of());
    }

    public RankingSnapshot(List<ReadUserPointResponse> sortedMembers, Set<Long> hidden) {
        var visible = sortedMembers.stream().filter(m -> !hidden.contains(m.getUserId())).toList();
        top20 = visible.stream().limit(20).toList();
        Map<Long, ReadUserPointResponse> memberMap = new HashMap<>();
        visible.forEach(member -> memberMap.put(member.getUserId(), member));
        members = Map.copyOf(memberMap);
        ranks = rankByPoints(visible);
        activityRanks = hidden.isEmpty() ? ranks : rankByPoints(sortedMembers);
    }

    private static Map<Long, Integer> rankByPoints(List<ReadUserPointResponse> sorted) {
        Map<Long, Integer> result = new HashMap<>();
        Integer previousPoints = null;
        int rank = 0;
        for (int i = 0; i < sorted.size(); i++) {
            var member = sorted.get(i);
            if (previousPoints == null || previousPoints != member.getTotalPoints()) rank = i + 1;
            result.put(member.getUserId(), rank);
            previousPoints = member.getTotalPoints();
        }
        return Map.copyOf(result);
    }

    public int activityRank(long userId) {
        return activityRanks.getOrDefault(userId, -1);
    }

    public ReadUserRankingResponse forUser(Long userId) {
        return ReadUserRankingResponse.of(top20, ranks.getOrDefault(userId, -1), members.get(userId));
    }
}
