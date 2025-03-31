package RIKU.server.Util;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class ConvertCreatedDate {
    public static String setCreatedDate(LocalDateTime createdAt) {
        LocalDateTime now = LocalDateTime.now();
        if(ChronoUnit.YEARS.between(createdAt, now) != 0)
            return ChronoUnit.YEARS.between(createdAt, now) + "년 전";
        else if(ChronoUnit.MONTHS.between(createdAt, now) != 0)
            return ChronoUnit.MONTHS.between(createdAt, now) + "달 전";
        else if(ChronoUnit.WEEKS.between(createdAt, now) != 0)
            return ChronoUnit.WEEKS.between(createdAt, now) + "주 전";
        else if(ChronoUnit.DAYS.between(createdAt, now) != 0)
            return ChronoUnit.DAYS.between(createdAt, now) + "일 전";
        else if(ChronoUnit.HOURS.between(createdAt, now) != 0)
            return ChronoUnit.HOURS.between(createdAt, now) + "시간 전";
        else if (ChronoUnit.MINUTES.between(createdAt, now) != 0)
            return ChronoUnit.MINUTES.between(createdAt, now) + "분 전";
        else
            return "방금";
    }
}
