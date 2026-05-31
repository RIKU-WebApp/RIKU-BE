package RIKU.server.domains.point.dto.response;

import RIKU.server.domains.point.entity.UserPoint;
import RIKU.server.global.util.DateTimeUtils;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
public class ReadPointListResponse {

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime date;

    private String tag;

    private String type;

    private String postTitle;

    private int point;

    private ReadPointListResponse(LocalDateTime date, String tag, String type, String postTitle, int point) {
        this.date = date;
        this.tag = tag;
        this.type = type;
        this.postTitle = postTitle;
        this.point = point;
    }

    public static ReadPointListResponse of(UserPoint userPoint, String tag) {
        return new ReadPointListResponse(
                userPoint.getCreatedAt(),
                tag,
                userPoint.getDescription(),
                userPoint.getPost() != null ? userPoint.getPost().getTitle() : null,
                userPoint.getPoint());
    }
}
