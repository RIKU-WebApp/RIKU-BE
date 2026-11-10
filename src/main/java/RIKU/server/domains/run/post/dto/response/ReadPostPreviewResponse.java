package RIKU.server.domains.run.post.dto.response;

import RIKU.server.domains.run.post.entity.Post;
import RIKU.server.domains.run.post.entity.PostStatus;
import RIKU.server.domains.run.post.repository.PostPreviewRow;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Getter
@NoArgsConstructor
public class ReadPostPreviewResponse {

    private Long id;

    private String title;

    private LocalDateTime date;

    private int participants; // 참가자 수

    private PostStatus postStatus;

    private String postImageUrl;
    private Boolean showFullImage;

    private ReadPostPreviewResponse(Long id, String title, LocalDateTime date, int participants, PostStatus postStatus, String postImageUrl, Boolean showFullImage) {
        this.id = id;
        this.title = title;
        this.date = date;
        this.participants = participants;
        this.postStatus = postStatus;
        this.postImageUrl = postImageUrl;
        this.showFullImage = showFullImage;
    }

    public static ReadPostPreviewResponse of (Post post, int participants) {
        return new ReadPostPreviewResponse(
                post.getId(),
                post.getTitle(),
                post.getDate(),
                participants,
                post.getPostStatus(),
                post.getPostImageUrl(), post.getShowFullImage()
        );
    }

    public static ReadPostPreviewResponse of(PostPreviewRow post, int participants) {
        return new ReadPostPreviewResponse(post.id(), post.title(), post.date(), participants, post.postStatus(), post.postImageUrl(), post.showFullImage());
    }
}
