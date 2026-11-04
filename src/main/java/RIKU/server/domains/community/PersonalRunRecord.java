package RIKU.server.domains.community;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
@Entity @Getter @NoArgsConstructor(access=AccessLevel.PROTECTED)
public class PersonalRunRecord {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    private Long userId;
    private LocalDate date;
    @Column(precision=7,scale=2) private BigDecimal distanceKm;
    private int minutes;
    private String title;
    public PersonalRunRecord(Long userId,LocalDate date,BigDecimal distanceKm,int minutes,String title) {
        this.userId=userId;this.date=date;this.distanceKm=distanceKm;this.minutes=minutes;this.title=title;
    }
}
