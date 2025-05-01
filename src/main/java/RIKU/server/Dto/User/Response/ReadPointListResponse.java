package RIKU.server.Dto.User.Response;

import RIKU.server.Entity.User.UserPoint;
import RIKU.server.Util.DateTimeUtils;
import lombok.Getter;

import java.time.LocalDate;

@Getter
public class ReadPointListResponse {

    private LocalDate date;

    private String tag;

    private String type;

    private String postTitle;

    private int point;

    private ReadPointListResponse(LocalDate date, String tag, String type, String postTitle, int point) {
        this.date = date;
        this.tag = tag;
        this.type = type;
        this.postTitle = postTitle;
        this.point = point;
    }

    public static ReadPointListResponse of(UserPoint userPoint, String tag) {
        return new ReadPointListResponse(
                DateTimeUtils.toUserLocalDate(userPoint.getCreatedAt()),
                tag,
                userPoint.getDescription(),
                userPoint.getPost() != null ? userPoint.getPost().getTitle() : null,
                userPoint.getPoint());
    }
}
