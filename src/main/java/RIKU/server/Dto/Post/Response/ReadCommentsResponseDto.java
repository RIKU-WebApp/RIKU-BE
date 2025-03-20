package RIKU.server.Dto.Post.Response;

import RIKU.server.Entity.Base.BaseStatus;
import RIKU.server.Entity.Board.Comment;
import lombok.*;

import java.util.List;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReadCommentsResponseDto {
    private Long commentId;

    private Long userId;
    private String userProfileImg;
    private String userName;

    private String content;
    private BaseStatus commentStatus;
    private List<ReadCommentsResponseDto> replies; // 대댓글 리스트

    public static ReadCommentsResponseDto of(Comment comment, List<ReadCommentsResponseDto> replies) {
        return ReadCommentsResponseDto.builder()
                .commentId(comment.getId())
                .userId(comment.getUser().getId())
                .userProfileImg(comment.getUser().getProfileImageUrl())
                .userName(comment.getUser().getName())
                .content(comment.getContent())
                .commentStatus(comment.getBaseStatus())
                .replies(replies)
                .build();
    }
}
