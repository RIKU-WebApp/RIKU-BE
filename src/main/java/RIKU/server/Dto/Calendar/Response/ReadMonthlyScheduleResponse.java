package RIKU.server.Dto.Calendar.Response;

import lombok.Getter;

import java.time.LocalDate;

@Getter
public class ReadMonthlyScheduleResponse {

    private LocalDate date;

    private Long eventCount;

    private ReadMonthlyScheduleResponse(LocalDate date, Long eventCount){
        this.date = date;
        this.eventCount = eventCount;
    }
    public static ReadMonthlyScheduleResponse of(LocalDate date, Long eventCount) {
        return new ReadMonthlyScheduleResponse(date, eventCount);
    }
}
