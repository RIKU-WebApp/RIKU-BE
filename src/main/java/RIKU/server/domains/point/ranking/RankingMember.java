package RIKU.server.domains.point.ranking;

import java.util.Set;

public record RankingMember(Long userId, String name, String profileImageUrl) {
    private static final Set<Long> HIDDEN_FROM_RANKING_TAB = Set.of(231L, 331L, 412L);

    public boolean hiddenFromRankingTab() {
        return HIDDEN_FROM_RANKING_TAB.contains(userId);
    }
}
