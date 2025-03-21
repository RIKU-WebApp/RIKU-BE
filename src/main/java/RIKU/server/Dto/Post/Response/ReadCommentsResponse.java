package RIKU.server.Dto.Post.Response;

import RIKU.server.Entity.Base.BaseStatus;
import RIKU.server.Entity.Board.Comment;
import lombok.*;

import java.util.List;

@Getter
@Builder
public class ReadCommentsResponse {

    private Long commentId;

    private Long userId;

    private String userProfileImg;

    private String userName;

    private String content;

    private BaseStatus commentStatus;

    private List<ReadCommentsResponse> replies; // 대댓글 리스트

    public static ReadCommentsResponse of(Comment comment, List<ReadCommentsResponse> replies) {
        return ReadCommentsResponse.builder()
                .commentId(comment.getId())
                .userId(comment.getUser().getId())
                .userProfileImg(comment.getUser().getProfileImageUrl())
                .userName(comment.getUser().getName())
                .content(comment.getContent())
                .commentStatus(comment.getStatus())
                .replies(replies)
                .build();
    }
}
