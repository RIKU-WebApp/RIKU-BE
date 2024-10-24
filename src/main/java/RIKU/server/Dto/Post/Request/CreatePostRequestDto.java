package RIKU.server.Dto.Post.Request;

import jakarta.persistence.Column;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreatePostRequestDto {

    @NotNull(message = "제목은 필수 항목입니다.")
    @Size(min = 1, message = "내용은 최소 1자 이상이어야 합니다.")
    private String title;

    private String location; // 집합 장소

    private LocalDateTime date; // 집합 날짜 및 시간

    @Column(columnDefinition = "Text")
    private String content; // 게시글 내용




}
