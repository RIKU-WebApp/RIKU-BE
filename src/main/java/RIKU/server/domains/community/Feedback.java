package RIKU.server.domains.community;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
@Entity @Getter @NoArgsConstructor(access=AccessLevel.PROTECTED)
public class Feedback {
    public enum Category { SUGGESTION, BUG, FEATURE }
    public enum Status { RECEIVED, REVIEWING, COMPLETED }
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false) private Long userId;
    @Enumerated(EnumType.STRING) private Category category;
    @Column(nullable=false,length=100) private String title;
    @Column(nullable=false,length=3000) private String content;
    @Enumerated(EnumType.STRING) private Status status;
    @Column(length=2000) private String answer;
    private Instant createdAt;
    public Feedback(Long userId,Category category,String title,String content) {
        this.userId=userId;this.category=category;this.title=title;this.content=content;
        this.status=Status.RECEIVED;this.createdAt=Instant.now();
    }
    public void respond(Status status,String answer) { this.status=status;this.answer=answer; }
}
