package RIKU.server.domains.point.ranking;

import RIKU.server.domains.point.dto.response.ReadUserPointResponse;
import RIKU.server.domains.point.dto.response.ReadUserRankingResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** No managed entities or caller-specific response is stored in the shared snapshot. */
public final class RankingSnapshot {
    private final List<ReadUserPointResponse> top20;
    private final Map<Long, ReadUserPointResponse> members;
    private final Map<Long, Integer> ranks;

    public RankingSnapshot(List<ReadUserPointResponse> sortedMembers) {
        top20 = sortedMembers.stream().limit(20).toList();
        Map<Long, ReadUserPointResponse> memberMap = new HashMap<>();
        Map<Long, Integer> rankMap = new HashMap<>();
        Integer previousPoints = null;
        int rank = 0;
        for (int i = 0; i < sortedMembers.size(); i++) {
            var member = sortedMembers.get(i);
            if (previousPoints == null || previousPoints != member.getTotalPoints()) rank = i + 1;
            memberMap.put(member.getUserId(), member);
            rankMap.put(member.getUserId(), rank);
            previousPoints = member.getTotalPoints();
        }
        members = Map.copyOf(memberMap);
        ranks = Map.copyOf(rankMap);
    }

    public ReadUserRankingResponse forUser(Long userId) {
        return ReadUserRankingResponse.of(top20, ranks.getOrDefault(userId, -1), members.get(userId));
    }
}
