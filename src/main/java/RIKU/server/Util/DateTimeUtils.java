package RIKU.server.Util;

import java.time.*;

public class DateTimeUtils {

    private static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Seoul");

    public static ZoneId getDefaultZone() {
        return DEFAULT_ZONE;
    }

    // UTC -> KST 변환
    public static ZonedDateTime toUserZonedTime(LocalDateTime utcTime) {
        return utcTime.atZone(ZoneOffset.UTC).withZoneSameInstant(DEFAULT_ZONE);
    }

    // KST 기준 LocalDate 반환
    public static LocalDate toUserLocalDate(LocalDateTime utcTime) {
        return toUserZonedTime(utcTime).toLocalDate();
    }

    // isToday (KST 기준)
    public static boolean isToday(LocalDateTime utcTime) {
        LocalDate now = LocalDate.now(DEFAULT_ZONE);
        return toUserLocalDate(utcTime).isEqual(now);
    }

    // isFuture (KST 기준)
    public static boolean isFuture(LocalDateTime utcTime) {
        LocalDateTime now = LocalDateTime.now(DEFAULT_ZONE);
        return toUserZonedTime(utcTime).toLocalDateTime().isAfter(now);
    }

    // 현재 KST 시간
    public static LocalDateTime nowKST() {
        return LocalDateTime.now(DEFAULT_ZONE);
    }

    public static LocalDateTime startOfTodayKST() {
        return LocalDate.now(DEFAULT_ZONE).atStartOfDay();
    }

    public static LocalDateTime endOfTodayKST() {
        return startOfTodayKST().plusDays(1).minusNanos(1);
    }
}
