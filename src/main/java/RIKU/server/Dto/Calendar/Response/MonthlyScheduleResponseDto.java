package RIKU.server.Dto.Calendar.Response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyScheduleResponseDto {

    private LocalDate date;
    private Long eventCount;

}
