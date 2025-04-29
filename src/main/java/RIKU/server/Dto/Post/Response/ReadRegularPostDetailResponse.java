package RIKU.server.Dto.Post.Response;

import RIKU.server.Dto.Participant.Response.GroupParticipantResponse;
import RIKU.server.Dto.User.Response.ReadUserInfoResponse;
import RIKU.server.Entity.Board.Post.Post;
import RIKU.server.Entity.Board.PostStatus;
import RIKU.server.Entity.Participant.ParticipantStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Builder
@Getter
public class ReadRegularPostDetailResponse {
    // 게시글 관련
    private Long id;
    private String title;
    private String location;
    private LocalDateTime date;

    private List<GroupParticipantResponse> groupedParticipants;
    private int participantsNum; // 참여자 수
    private int attendedParticipantsNum; // 출석 완료한 참여자 수

    private List<ReadPacersListResponse> pacers;
    private ReadUserInfoResponse postCreatorInfo;
    private String content;
    private PostStatus postStatus;
    private String postImageUrl;
    private List<String> attachmentUrls;

    // 유저 관련
    private ReadUserInfoResponse userInfo;

    // 댓글 관련
    private List<ReadCommentsResponse> comments;

    public static ReadRegularPostDetailResponse of (Post post, ReadUserInfoResponse postCreator, ReadUserInfoResponse user, List<GroupParticipantResponse> groupedParticipants, List<ReadPacersListResponse> pacers, List<String> attachmentUrls, List<ReadCommentsResponse> comments) {
        return ReadRegularPostDetailResponse.builder()
                .id(post.getId())
                .title(post.getTitle())
                .location(post.getLocation())
                .date(post.getDate())
                .groupedParticipants(groupedParticipants)
                .participantsNum(groupedParticipants.stream().mapToInt(g -> g.getParticipants().size()).sum())
                .attendedParticipantsNum((int) groupedParticipants.stream()
                        .flatMap(g -> g.getParticipants().stream())
                        .filter(p -> p.getStatus() == ParticipantStatus.ATTENDED)
                        .count())
                .pacers(pacers)
                .postCreatorInfo(postCreator)
                .content(post.getContent())
                .postStatus(post.getPostStatus())
                .postImageUrl(post.getPostImageUrl())
                .attachmentUrls(attachmentUrls)
                .userInfo(user)
                .comments(comments)
                .build();
    }
}
