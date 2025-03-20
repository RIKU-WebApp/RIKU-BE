package RIKU.server.Dto.Calendar.Response;

import RIKU.server.Entity.Board.Post.Post;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ReadDailyScheduleResponse {

    private Long postId;

    private String title;

    private LocalDateTime date;

    private String location;

    private ReadDailyScheduleResponse(Long postId, String title, LocalDateTime date, String location) {
        this.postId = postId;
        this.title = title;
        this.date = date;
        this.location = location;
    }

    public static ReadDailyScheduleResponse of(Post post) {
        return new ReadDailyScheduleResponse(
                post.getId(),
                post.getTitle(),
                post.getDate(),
                post.getLocation()
        );
    }
}
