package RIKU.server.Entity.Board;

import RIKU.server.Entity.Base.BaseEntity;
import RIKU.server.Entity.Board.Post.Post;
import RIKU.server.Entity.User.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static jakarta.persistence.FetchType.LAZY;

@Entity
@Table(name = "comment")
@Getter
@NoArgsConstructor
public class Comment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comment_id")
    private Long id;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "post_id")
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Lob
    private String content;

    @Column(name = "target_id")
    private Long targetId;

    private Comment(User user, Post post, String content, Long targetId) {
        this.user = user;
        this.post = post;
        this.content = content;
        this.targetId = targetId;
    }

    public static Comment create(User user, Post post, String content, Long targetId) {
        return new Comment(user, post, content, targetId);
    }
}
