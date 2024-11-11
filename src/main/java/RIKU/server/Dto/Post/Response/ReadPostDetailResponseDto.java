package RIKU.server.Dto.Post.Response;

import RIKU.server.Entity.Board.Post;
import RIKU.server.Entity.Board.PostStatus;
import RIKU.server.Entity.Participant.Participant;
import RIKU.server.Entity.User.User;
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
    private List<Participant> participants;
    private int participantsNum; // 참가자 수
    private String content;
    private PostStatus postStatus;

    // 유저 관련
    private Long userId;
    private String userProfileImg;
    private String userName;

    public static ReadPostDetailResponseDto of (Post post) {
        return ReadPostDetailResponseDto.builder()
                .title(post.getTitle())
                .location(post.getLocation())
                .date(post.getDate())
                .participants(post.getParticipants())
                .participantsNum(post.getParticipants().size())
                .content(post.getContent())
                .postStatus(post.getPostStatus())
                .userId(post.getCreatedBy().getId())
                .userProfileImg(post.getCreatedBy().getImageUrl())
                .userName(post.getCreatedBy().getName())
                .build();
    }
}
