package RIKU.server.domains.run.post.dto.response;

import RIKU.server.domains.run.comment.dto.response.ReadCommentsResponse;

import RIKU.server.domains.run.participant.dto.response.ReadParticipantListResponse;
import RIKU.server.domains.user.dto.response.ReadUserInfoResponse;
import RIKU.server.domains.run.post.entity.EventPost;
import RIKU.server.domains.run.post.entity.Post;
import RIKU.server.domains.run.post.entity.PostStatus;
import RIKU.server.domains.run.participant.entity.ParticipantStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Builder
@Getter
public class ReadEventPostDetailResponse {
    // 게시글 관련
    private Long id;
    private String title;
    private String location;
    private LocalDateTime date;
    private String eventType;
    private List<ReadParticipantListResponse> participants;
    private int participantsNum; // 참여자 수
    private int attendedParticipantsNum; // 출석 완료한 참여자 수
    private ReadUserInfoResponse PostCreatorInfo;
    private String content;
    private PostStatus postStatus;
    private String postImageUrl;
    private List<String> attachmentUrls;

    // 유저 관련
    private ReadUserInfoResponse userInfo;

    // 댓글 관련
    private List<ReadCommentsResponse> comments;

    public static ReadEventPostDetailResponse of (Post post, EventPost eventPost, List<ReadParticipantListResponse> participants, ReadUserInfoResponse postCreator, List<String> attachmentUrls, ReadUserInfoResponse user, List<ReadCommentsResponse> comments) {
        return ReadEventPostDetailResponse.builder()
                .id(post.getId())
                .title(post.getTitle())
                .location(post.getLocation())
                .date(post.getDate())
                .eventType(eventPost.getEventType())
                .participants(participants)
                .participantsNum(participants.size())
                .attendedParticipantsNum((int) participants.stream()
                        .filter(p -> p.getStatus() == ParticipantStatus.ATTENDED)
                        .count())
                .PostCreatorInfo(postCreator)
                .content(post.getContent())
                .postStatus(post.getPostStatus())
                .postImageUrl(post.getPostImageUrl())
                .attachmentUrls(attachmentUrls)
                .userInfo(user)
                .comments(comments)
                .build();
    }
}
