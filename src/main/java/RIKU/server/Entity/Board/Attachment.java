package RIKU.server.Entity.Board;

import RIKU.server.Entity.Base.BaseEntity;
import RIKU.server.Entity.Board.Post.Post;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "attachment")
@Getter
@NoArgsConstructor
public class Attachment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "attachment_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Post post;

    @Column(name = "image_url")
    private String imageUrl;

    private Attachment(Post post, String imageUrl) {
        this.post = post;
        this.imageUrl = imageUrl;
    }

    public static Attachment create(Post post, String imageUrl) {
        return new Attachment(post, imageUrl);
    }
}
