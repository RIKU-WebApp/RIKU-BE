package RIKU.server.Dto.Post.Response;

import RIKU.server.Dto.Participant.Response.ParticipantListResponseDto;
import RIKU.server.Entity.Board.PostStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ReadPostDetailResponseDto {
    // 게시글 관련
    private String title;
    private String location;
    private LocalDateTime date;
    private List<ParticipantListResponseDto> participants;
    private int participantsNum; // 참가자 수
    private String content;
    private PostStatus postStatus;
    private String postImageUrl;

    // 유저 관련
    private Long userId;
    private String userProfileImg;
    private String userName;

    // 댓글 관련
    private List<ReadCommentsResponseDto> comments;

}
