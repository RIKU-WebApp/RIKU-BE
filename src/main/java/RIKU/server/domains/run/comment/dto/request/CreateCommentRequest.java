package RIKU.server.domains.run.comment.dto.request;

import RIKU.server.domains.run.comment.entity.Comment;
import RIKU.server.domains.run.post.entity.Post;
import RIKU.server.domains.user.entity.User;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CreateCommentRequest {

    @NotNull(message = "내용은 필수 항목입니다.")
    private String content;

    private Long targetId;

    public Comment toEntity(User user, Post post, Long targetId) {
        return Comment.create(user, post, this.content, targetId);
    }
}
