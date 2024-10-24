package RIKU.server.Dto.Post.Response;

import RIKU.server.Entity.Board.Post;
import RIKU.server.Entity.Board.PostStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ReadPostsResponseDto {

    private String title;
    private LocalDateTime date;
    private int participants; // 참가자 수
    private PostStatus postStatus;
    private String postImages;

    public static ReadPostsResponseDto of (Post post) {
        return ReadPostsResponseDto.builder()
                .title(post.getTitle())
                .date(post.getDate())
                .participants(post.getParticipants().size())
                .postStatus(post.getPostStatus())
                .build();
    }


}
