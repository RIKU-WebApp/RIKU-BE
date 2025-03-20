package RIKU.server.Dto.Post.Request;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class UpdatePostRequest {

    @Nullable
    private String eventType;

    @Nullable
    private String trainingType;

    @Nullable
    @Size(min = 1, message = "내용은 최소 1자 이상이어야 합니다.")
    private String title;      // 게시글 제목

    @Nullable
    private String location;    // 집합 장소

    @Nullable
    @Future(message = "미래 날짜만 입력가능합니다.")
    private LocalDateTime date;     // 집합 날짜 및 시간

    @Nullable
    private List<UpdatePacerRequest> pacers;

    @Nullable
    private String content;     // 게시글 내용

    @Nullable
    private MultipartFile postImage;    // 게시글 이미지

    @Nullable
    private List<MultipartFile> attachments;    // 게시글 첨부파일
}
