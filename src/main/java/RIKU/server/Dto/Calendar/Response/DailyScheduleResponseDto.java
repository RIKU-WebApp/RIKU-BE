package RIKU.server.Dto.Calendar.Response;

import RIKU.server.Entity.Board.Post;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class DailyScheduleResponseDto {

    private Long postId;
    private String title;
    private LocalDateTime date;
    private String location;

    public static DailyScheduleResponseDto of(Post post) {
        return DailyScheduleResponseDto.builder()
                .postId(post.getId())
                .title(post.getTitle())
                .date(post.getDate())
                .location(post.getLocation())
                .build();
    }

}
