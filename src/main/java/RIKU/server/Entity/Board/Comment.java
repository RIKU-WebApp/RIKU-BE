package RIKU.server.Entity.Board;

import RIKU.server.Entity.BaseEntity;
import RIKU.server.Entity.BaseStatus;
import RIKU.server.Entity.User.User;
import jakarta.persistence.*;
import lombok.Builder;
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

    @Column(columnDefinition = "Text")
    private String content;

    @Column(name = "target_id")
    private Long targetId;

    @Enumerated(EnumType.STRING)
    @Column(name = "base_status")
    private BaseStatus baseStatus = BaseStatus.ACTIVE;

    @Builder
    public Comment(User user, Post post, String content, Comment targetComment) {
        this.user = user;
        this.post = post;
        this.content = content;
        this.targetId = targetComment != null ? targetComment.getId() : null;
    }

    public void updateInactive() {
        this.baseStatus = BaseStatus.INACTIVE;
    }
}
