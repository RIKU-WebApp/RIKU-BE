package RIKU.server.Dto.Post.Request;

import RIKU.server.Entity.Board.Post.Post;
import RIKU.server.Entity.Board.Post.PostType;
import RIKU.server.Entity.Board.Post.RegularPost;
import RIKU.server.Entity.User.User;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class CreateRegularPostRequest {

    @NotBlank(message = "제목은 필수 항목입니다.")
    @Size(min = 1, message = "내용은 최소 1자 이상이어야 합니다.")
    private String title;      // 게시글 제목

    @NotBlank(message = "집합 장소는 필수 항목입니다.")
    private String location;    // 집합 장소

    @NotNull(message = "집합 날짜 및 시간은 필수 항목입니다.")
    @Future(message = "미래 날짜만 입력가능합니다.")
    private LocalDateTime date;     // 집합 날짜 및 시간

    @NotNull(message = "페이서 정보는 필수 항목입니다.")
    @Size(min = 1, message = "페이서는 최소 한 명 이상이어야 합니다.")
    private List<CreatePacerRequest> pacers;

    @NotBlank(message = "내용은 필수 항목입니다.")
    private String content;     // 게시글 내용

    @Nullable
    private MultipartFile postImage;    // 게시글 이미지

    @Nullable
    private List<MultipartFile> attachments;    // 게시글 첨부파일

    public Post toPostEntity(User user, String postImageUrl) {
        return Post.create(
                user,
                this.getTitle(),
                this.getLocation(),
                this.getDate(),
                this.getContent(),
                postImageUrl,
                PostType.REGULAR);
    }

    public RegularPost toRegularPostEntity(Post post) {
        return RegularPost.create(post);
    }
}
