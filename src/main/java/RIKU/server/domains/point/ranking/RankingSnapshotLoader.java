package RIKU.server.domains.point.ranking;

import RIKU.server.domains.point.dto.response.ReadUserPointResponse;
import RIKU.server.domains.point.repository.UserPointRepository;
import RIKU.server.domains.user.repository.UserRepository;
import RIKU.server.global.entity.BaseStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.text.Collator;
import java.util.Comparator;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RankingSnapshotLoader {
    private static final Set<Long> EXCLUDED_USER_IDS = Set.of(10L, 29L, 6L, 79L);
    private final UserRepository userRepository;
    private final UserPointRepository userPointRepository;

    // Never populate a shared cache from the caller's potentially older transaction snapshot.
    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public RankingSnapshot load(RankingQuery query) {
        var members = userRepository.findRankingMembers(BaseStatus.ACTIVE, EXCLUDED_USER_IDS);
        var totals = query.endExclusive() == null
                ? userPointRepository.sumByUserSince(query.start())
                : userPointRepository.sumByUserInEvent(query.start(), query.endExclusive(), query.pointTypes());
        Map<Long, Long> byUser = totals.stream().collect(Collectors.toMap(RankingTotal::userId, RankingTotal::points));
        // Collator is mutable and not thread-safe: keep one per calculation.
        Collator collator = Collator.getInstance(Locale.KOREAN);
        Comparator<ReadUserPointResponse> order = Comparator.comparingInt(ReadUserPointResponse::getTotalPoints).reversed()
                .thenComparing((left, right) -> compareNames(left.getUserName(), right.getUserName(), collator))
                .thenComparing(ReadUserPointResponse::getUserId);
        var sorted = members.stream()
                .map(member -> ReadUserPointResponse.of(member.userId(), member.profileImageUrl(), member.name(),
                        Math.toIntExact(byUser.getOrDefault(member.userId(), 0L))))
                .sorted(order).toList();
        var hidden = members.stream().filter(RankingMember::hiddenFromRankingTab)
                .map(RankingMember::userId).collect(Collectors.toSet());
        return new RankingSnapshot(sorted, hidden);
    }

    private int compareNames(String left, String right, Collator collator) {
        String l = Objects.toString(left, "").trim();
        String r = Objects.toString(right, "").trim();
        int category = Integer.compare(nameCategory(l), nameCategory(r));
        return category != 0 ? category : collator.compare(l, r);
    }

    private int nameCategory(String name) {
        if (name.isEmpty()) return 2;
        char ch = name.charAt(0);
        return (ch >= '\uAC00' && ch <= '\uD7A3') || (ch >= '\u3131' && ch <= '\u318E') ? 0 : 1;
    }
}
