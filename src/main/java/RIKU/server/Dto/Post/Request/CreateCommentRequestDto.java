package RIKU.server.Dto.Post.Request;

import RIKU.server.Entity.Board.Comment;
import RIKU.server.Entity.Board.Post;
import RIKU.server.Entity.User.User;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CreateCommentRequestDto {

    @NotNull(message = "내용은 필수 항목입니다.")
    private String content;

    private Long targetId;

    public Comment toEntity(User user, Post post, Comment targetComment) {
        return Comment.builder()
                .user(user)
                .post(post)
                .content(content)
                .targetComment(targetComment)
                .build();
    }

}
