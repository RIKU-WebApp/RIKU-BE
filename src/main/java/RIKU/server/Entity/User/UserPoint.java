package RIKU.server.Entity.User;

import RIKU.server.Entity.Base.BaseEntity;
import RIKU.server.Entity.Base.BaseStatus;
import RIKU.server.Entity.Board.PostStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_point")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserPoint extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "point_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private int point;  // 적립(+) or 차감(-) 포인트 값

    private String description;

    @Column(name = "point_type")
    @Enumerated(EnumType.STRING)
    private PointType pointType;    // 적립(add), 차감(remove)

    private UserPoint(User user, int point, String description, PointType pointType) {
        this.user = user;
        this.point = point;
        this.description = description;
        this.pointType = pointType;
    }

    public static UserPoint create(User user, int point, String description, PointType pointType) {
        return new UserPoint(user, point, description, pointType);
    }
}
