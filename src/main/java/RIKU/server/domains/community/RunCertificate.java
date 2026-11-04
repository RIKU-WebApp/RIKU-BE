package RIKU.server.domains.community;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Entity
@Table(indexes = @Index(name = "ix_certificate_owner", columnList = "user_id,id"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RunCertificate {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Column(nullable = false, length = 120)
    private String title;
    @Column(nullable = false)
    private LocalDate date;
    @Lob @Column(nullable = false, columnDefinition = "MEDIUMBLOB")
    private byte[] image;

    RunCertificate(Long userId, String title, LocalDate date, byte[] image) {
        this.userId = userId;
        this.title = title;
        this.date = date;
        this.image = image;
    }
    Long id() { return id; }
}
