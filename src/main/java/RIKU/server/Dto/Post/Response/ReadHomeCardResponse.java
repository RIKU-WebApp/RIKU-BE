package RIKU.server.Dto.Post.Response;

import RIKU.server.Entity.Board.Post.Post;
import RIKU.server.Entity.Board.PostStatus;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ReadHomeCardResponse {

    private String title;

    private LocalDateTime date;

    private String postImageUrl;

    private PostStatus postStatus;

    private ReadHomeCardResponse (String title, LocalDateTime date, String postImageUrl, PostStatus postStatus) {
        this.title = title;
        this.date = date;
        this.postImageUrl = postImageUrl;
        this.postStatus = postStatus;
    }

    public static ReadHomeCardResponse of(Post post) {
        return new ReadHomeCardResponse(
                post.getTitle(),
                post.getDate(),
                post.getPostImageUrl(),
                post.getPostStatus()
        );
    }
}
