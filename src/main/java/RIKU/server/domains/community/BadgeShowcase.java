package RIKU.server.domains.community;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

@Entity
@Table(name = "badge_showcase")
class BadgeShowcase {
    @Id @Column(name = "user_id")
    private Long userId;
    private String medals;
    private String badges;

    protected BadgeShowcase() {}

    BadgeShowcase(long userId, Selection selection) {
        this.userId = userId;
        medals = encode(selection.medals());
        badges = encode(selection.badges());
    }

    record Selection(List<String> medals, List<String> badges) {}

    Selection selection() {
        return new Selection(decode(medals), decode(badges));
    }

    // Null follows the automatic preview. An empty list explicitly hides this category.
    private static String encode(List<String> keys) {
        return keys == null ? null : String.join(",", keys);
    }

    private static List<String> decode(String value) {
        return value == null ? null : value.isEmpty() ? List.of() : List.of(value.split(","));
    }
}

interface BadgeShowcaseRepository extends JpaRepository<BadgeShowcase, Long> {}
