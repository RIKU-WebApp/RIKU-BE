package RIKU.server.Dto.Post.Response;

import RIKU.server.Dto.Participant.Response.ReadParticipantListResponse;
import RIKU.server.Entity.Board.Post.Post;
import RIKU.server.Entity.Board.PostStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Builder
@Getter
public class ReadFlashPostDetailResponse {
    // 게시글 관련
    private String title;
    private String location;
    private LocalDateTime date;
    private List<ReadParticipantListResponse> participants;
    private int participantsNum; // 참가자 수
    private String content;
    private PostStatus postStatus;
    private String postImageUrl;
    private List<String> attachmentUrls;

    // 유저 관련
    private Long userId;
    private String userProfileImg;
    private String userName;

    // 댓글 관련
    private List<ReadCommentsResponse> comments;

    public static ReadFlashPostDetailResponse of (Post post, List<ReadParticipantListResponse> participants, List<String> attachmentUrls, List<ReadCommentsResponse> comments) {
        return ReadFlashPostDetailResponse.builder()
                .title(post.getTitle())
                .location(post.getLocation())
                .date(post.getDate())
                .participants(participants)
                .participantsNum(participants.size())
                .content(post.getContent())
                .postStatus(post.getPostStatus())
                .postImageUrl(post.getPostImageUrl())
                .attachmentUrls(attachmentUrls)
                .userId(post.getPostCreator().getId())
                .userProfileImg(post.getPostCreator().getProfileImageUrl())
                .userName(post.getPostCreator().getName())
                .comments(comments)
                .build();
    }
}
