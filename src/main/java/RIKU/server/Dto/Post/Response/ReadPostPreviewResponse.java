package RIKU.server.Dto.Post.Response;

import RIKU.server.Entity.Board.Post.Post;
import RIKU.server.Entity.Board.PostStatus;
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

    private ReadPostPreviewResponse(Long id, String title, LocalDateTime date, int participants, PostStatus postStatus, String postImageUrl) {
        this.id = id;
        this.title = title;
        this.date = date;
        this.participants = participants;
        this.postStatus = postStatus;
        this.postImageUrl = postImageUrl;
    }

    public static ReadPostPreviewResponse of (Post post, int participants) {
        return new ReadPostPreviewResponse(
                post.getId(),
                post.getTitle(),
                post.getDate(),
                participants,
                post.getPostStatus(),
                post.getPostImageUrl()
        );
    }
}
