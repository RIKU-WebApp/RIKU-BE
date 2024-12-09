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
public class ReadHomeResponseDto {

    private String runType;
    private String location;
    private LocalDateTime date;
    private String postImageUrl;
    private PostStatus postStatus;

    public static ReadHomeResponseDto of (String runType, Post post) {
        return ReadHomeResponseDto.builder()
                .runType(runType)
                .location(post.getLocation())
                .date(post.getDate())
                .postImageUrl(post.getPostImageUrl())
                .postStatus(post.getPostStatus())
                .build();
    }
}
