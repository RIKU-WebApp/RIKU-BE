package RIKU.server.Dto.Calendar.Response;

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

    private String title;
    private LocalDateTime time;
    private String location;

}
