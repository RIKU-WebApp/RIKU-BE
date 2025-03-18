package RIKU.server.Dto.Post.Request;

import RIKU.server.Entity.Board.Comment;
import RIKU.server.Entity.Board.Post.Post;
import RIKU.server.Entity.User.User;
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
