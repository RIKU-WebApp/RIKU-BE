package RIKU.server.Entity.Board;

import RIKU.server.Entity.Base.BaseEntity;
import RIKU.server.Entity.Board.Post.Post;
import RIKU.server.Entity.User.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "pacer")
@Getter
@NoArgsConstructor
public class Pacer extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pacer_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;  // 참여한 사용자

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Post post;

    private String group;

    private String pace;

    private String distance;


    private Pacer (User user, Post post, String group, String pace, String distance) {
        this.user = user;
        this.post = post;
        this.group = group;
        this.pace = pace;
        this.distance = distance;
    }

    public static Pacer create (User user, Post post, String group, String pace, String distance) {
        return new Pacer(user, post, group, pace, distance);
    }
}
