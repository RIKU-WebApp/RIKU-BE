package RIKU.server.Util;

import java.time.*;

public class DateTimeUtils {

    private static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Seoul");

    // 기본 KST Zone 반환
    public static ZoneId getDefaultZone() {
        return DEFAULT_ZONE;
    }

    // 현재 시각 (KST 기준)
    public static LocalDateTime nowKST() {
        return LocalDateTime.now(DEFAULT_ZONE);
    }

    // 오늘의 시작과 끝 (KST 기준)
    public static LocalDateTime startOfTodayKST() {
        return LocalDate.now(DEFAULT_ZONE).atStartOfDay();
    }

    public static LocalDateTime endOfTodayKST() {
        return startOfTodayKST().plusDays(1).minusNanos(1);
    }

    // KST → UTC 변환
    public static LocalDateTime toUtcTime(ZonedDateTime kstTime) {
        return kstTime.withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }

    public static LocalDateTime startOfDayUtc(LocalDate kstDate) {
        return toUtcTime(kstDate.atStartOfDay(DEFAULT_ZONE));
    }

    public static LocalDateTime endOfDayUtc(LocalDate kstDate) {
        return toUtcTime(kstDate.atTime(23, 59, 59).atZone(DEFAULT_ZONE));
    }

    // UTC -> KST 변환
    public static ZonedDateTime toUserZonedTime(LocalDateTime utcTime) {
        return utcTime.atZone(ZoneOffset.UTC).withZoneSameInstant(DEFAULT_ZONE);
    }

    public static LocalDate toUserLocalDate(LocalDateTime utcTime) {
        return toUserZonedTime(utcTime).toLocalDate();
    }

    // 시간 비교
    public static boolean isToday(LocalDateTime utcTime) {
        LocalDate now = LocalDate.now(DEFAULT_ZONE);
        return toUserLocalDate(utcTime).isEqual(now);
    }

    public static boolean isFuture(LocalDateTime utcTime) {
        LocalDateTime now = LocalDateTime.now(DEFAULT_ZONE);
        return toUserZonedTime(utcTime).toLocalDateTime().isAfter(now);
    }
}
