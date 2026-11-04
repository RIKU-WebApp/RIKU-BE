package RIKU.server.domains.community;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
@Entity @Getter @NoArgsConstructor(access=AccessLevel.PROTECTED)
@Table(uniqueConstraints=@UniqueConstraint(columnNames={"user_id","name"}))
public class BadgeAward {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="user_id",nullable=false) private Long userId;
    @Column(nullable=false,length=40) private String name;
    @Column(length=200) private String description;
    private Long awardedBy;
    private Instant awardedAt;
    public BadgeAward(Long userId,String name,String description,Long awardedBy) {
        this.userId=userId;this.name=name;this.description=description;this.awardedBy=awardedBy;this.awardedAt=Instant.now();
    }
}
