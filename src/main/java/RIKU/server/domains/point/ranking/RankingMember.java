package RIKU.server.domains.point.ranking;

import RIKU.server.domains.user.entity.UserRole;
import java.util.Set;

public record RankingMember(Long userId, String name, String profileImageUrl, UserRole role) {
    private static final Set<String> HIDDEN_ADMINS = Set.of("박건영", "이찬호", "박준한");

    public boolean hiddenFromRankingTab() {
        return hiddenFromRankingTab(name, role);
    }

    public static boolean hiddenFromRankingTab(String name, UserRole role) {
        return role == UserRole.ADMIN && name != null && HIDDEN_ADMINS.contains(name);
    }
}
