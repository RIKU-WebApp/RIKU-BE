package RIKU.server.domains.auth.recovery;

import RIKU.server.domains.community.Feedback;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(indexes = @Index(name = "ix_password_inquiry_student_time", columnList = "student_id, created_at"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PasswordInquiry {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "student_id", nullable = false)
    private String studentId;
    @Column(nullable = false)
    private String name;
    @Column(nullable = false)
    private String major;
    @Column(length = 11)
    private String phone;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 32)
    private Feedback.Status status;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    PasswordInquiry(String studentId, String name, String major, String phone) {
        this.studentId = studentId.trim();
        this.name = name.trim();
        this.major = major.trim();
        this.phone = phone == null || phone.isEmpty() ? null : phone.replace("-", "");
        this.status = Feedback.Status.RECEIVED;
        this.createdAt = Instant.now();
    }

    void updateStatus(Feedback.Status status) {
        this.status = status;
    }
}
