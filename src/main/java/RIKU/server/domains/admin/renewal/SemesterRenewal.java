package RIKU.server.domains.admin.renewal;

import RIKU.server.domains.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Entity
@Table(name = "semester_renewal", uniqueConstraints = @UniqueConstraint(columnNames = {"term", "member_id"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SemesterRenewal {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 6)
    private String term;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id")
    private User member;

    @Column(name = "confirmed_by", nullable = false)
    private Long confirmedBy;

    @Column(name = "confirmed_at", nullable = false)
    private Instant confirmedAt;

    SemesterRenewal(String term, User member, Long confirmedBy) {
        this.term = term;
        this.member = member;
        this.confirmedBy = confirmedBy;
        this.confirmedAt = Instant.now();
    }
}
