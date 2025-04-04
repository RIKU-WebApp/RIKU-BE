package RIKU.server.Entity.User;

import RIKU.server.Entity.Base.BaseEntity;
import RIKU.server.Entity.Board.Post.Post;
import jakarta.annotation.Nullable;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    @Nullable
    private Post post;

    private UserPoint(User user, int point, String description, PointType pointType) {
        this.user = user;
        this.point = point;
        this.description = description;
        this.pointType = pointType;
    }

    public static UserPoint create(User user, int point, String description, PointType pointType) {
        return new UserPoint(user, point, description, pointType);
    }

    public static UserPoint createWithPost(User user, int point, String description, PointType pointType, Post post) {
        UserPoint userPoint = new UserPoint(user, point, description, pointType);
        userPoint.post = post;
        return userPoint;
    }
}
