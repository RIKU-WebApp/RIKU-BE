package RIKU.server.Dto.User.Response;

import RIKU.server.Entity.User.UserPoint;
import RIKU.server.Util.DateTimeUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

@Getter
@Slf4j
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
        LocalDateTime createdAt = userPoint.getCreatedAt();
        ZonedDateTime utcZoned = createdAt.atZone(ZoneOffset.UTC);
        ZonedDateTime kstZoned = utcZoned.withZoneSameInstant(DateTimeUtils.getDefaultZone());
        LocalDate kstDate = kstZoned.toLocalDate();

        log.info("🟡 Raw createdAt: {}", createdAt);
        log.info("🟢 UTC ZonedDateTime: {}", utcZoned);
        log.info("🔵 KST ZonedDateTime: {}", kstZoned);
        log.info("📅 Final LocalDate (KST): {}", kstDate);

        return new ReadPointListResponse(
                DateTimeUtils.toUserLocalDate(userPoint.getCreatedAt()),
                tag,
                userPoint.getDescription(),
                userPoint.getPost() != null ? userPoint.getPost().getTitle() : null,
                userPoint.getPoint());
    }
}
