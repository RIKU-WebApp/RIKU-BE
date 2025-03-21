package RIKU.server.Dto.Post.Response;

import RIKU.server.Dto.Participant.Response.ReadParticipantListResponse;
import RIKU.server.Entity.Board.Post.Post;
import RIKU.server.Entity.Board.Post.TrainingPost;
import RIKU.server.Entity.Board.PostStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Builder
@Getter
public class ReadTrainingPostDetailResponse {
    // 게시글 관련
    private String title;
    private String location;
    private LocalDateTime date;
    private String trainingType;
    private List<ReadParticipantListResponse> participants;
    private int participantsNum; // 참가자 수
    private List<ReadPacersListResponse> pacers;
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

    public static ReadTrainingPostDetailResponse of (Post post, TrainingPost trainingPost, List<ReadParticipantListResponse> participants, List<ReadPacersListResponse> pacers, List<String> attachmentUrls, List<ReadCommentsResponse> comments) {
        return ReadTrainingPostDetailResponse.builder()
                .title(post.getTitle())
                .location(post.getLocation())
                .date(post.getDate())
                .trainingType(trainingPost.getTrainingType())
                .participants(participants)
                .participantsNum(participants.size())
                .pacers(pacers)
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
