package RIKU.server.Dto.Post.Response;

import RIKU.server.Entity.Base.BaseStatus;
import RIKU.server.Entity.Board.Comment;
import lombok.*;

import java.util.List;

@Getter
public class ReadCommentsResponseDto {

    private Long commentId;

    private Long userId;

    private String userProfileImg;

    private String userName;

    private String content;

    private BaseStatus commentStatus;

    private List<ReadCommentsResponseDto> replies; // 대댓글 리스트

}
