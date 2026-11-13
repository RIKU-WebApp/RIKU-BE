package RIKU.server.domains.community;

import RIKU.server.domains.community.BadgeShowcase.Selection;
import RIKU.server.domains.community.CommunityService.Badge;
import RIKU.server.domains.community.CommunityService.BadgePreview;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class BadgeShowcaseService {
    private final BadgeShowcaseRepository preferences;
    private final CommunityService community;
    private final RankHonors honors;

    Selection selection(long userId) {
        return preferences.findById(userId).map(BadgeShowcase::selection).orElse(new Selection(null, null));
    }

    record Editor(Selection selection, BadgePreview defaults) {}

    Editor editor(long userId) {
        return new Editor(selection(userId), community.badgePreview(userId));
    }

    BadgePreview preview(long userId) {
        var selection = selection(userId);
        var defaults = community.badgePreview(userId);
        var badges = selection.badges() == null ? defaults.badges()
                : ordered(selection.badges(), community.earnedBadges(userId), Badge::code);
        var medals = selection.medals() == null ? defaults.medals()
                : ordered(selection.medals(), honors.issuedForMember(userId), BadgeShowcaseService::medalKey);
        return new BadgePreview(badges, medals, defaults.medalCount());
    }

    record PublicDisplay(String name, List<Badge> badges, List<RankHonors.Medal> medals,
                         @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
                         Selection featured) {}

    PublicDisplay publicProfile(long userId) {
        var profile = community.profile(userId);
        var saved = selection(userId);
        // Only expose identifiers already present in the public collection.
        var medals = saved.medals() == null ? null : ordered(saved.medals(), profile.medals(), BadgeShowcaseService::medalKey)
                .stream().map(BadgeShowcaseService::medalKey).toList();
        var badges = saved.badges() == null ? null : ordered(saved.badges(), profile.badges(), Badge::code)
                .stream().map(Badge::code).toList();
        var featured = medals == null && badges == null ? null : new Selection(medals, badges);
        return new PublicDisplay(profile.name(), profile.badges(), profile.medals(), featured);
    }

    @Transactional
    public Selection save(long userId, Selection requested) {
        if (requested.badges() != null) {
            validate(requested.badges(), 6);
            requireEarned(requested.badges(), community.earnedBadges(userId), Badge::code);
        }
        if (requested.medals() != null) {
            validate(requested.medals(), 2);
            requireEarned(requested.medals(), honors.issuedForMember(userId), BadgeShowcaseService::medalKey);
        }
        if (requested.medals() == null && requested.badges() == null) preferences.deleteById(userId);
        else preferences.save(new BadgeShowcase(userId, requested));
        return requested;
    }

    private static void validate(List<String> keys, int limit) {
        if (keys.size() > limit || new HashSet<>(keys).size() != keys.size()
                || keys.stream().anyMatch(key -> key == null || key.isBlank()))
            throw new ResponseStatusException(BAD_REQUEST, "대표 배지 선택을 확인해 주세요.");
    }

    private static String medalKey(RankHonors.Medal medal) {
        return medal.kind() + medal.start();
    }

    private static <T> List<T> ordered(List<String> selected, List<T> earned, Function<T, String> key) {
        var available = earned.stream().collect(Collectors.toMap(key, Function.identity()));
        return selected.stream().map(available::get).filter(Objects::nonNull).toList();
    }

    private static <T> void requireEarned(List<String> selected, List<T> earned, Function<T, String> key) {
        if (ordered(selected, earned, key).size() != selected.size())
            throw new ResponseStatusException(BAD_REQUEST, "획득한 배지만 선택할 수 있습니다.");
    }
}
