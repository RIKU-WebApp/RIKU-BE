package RIKU.server.Controller;

import RIKU.server.Dto.Calendar.Response.ReadDailyScheduleResponse;
import RIKU.server.Dto.Calendar.Response.ReadMonthlyScheduleResponse;
import RIKU.server.Service.CalendarService;
import RIKU.server.Util.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/calendar")
@Tag(name = "Calendar", description = "캘린더(일정) 관련 API")
public class CalendarController {

    private final CalendarService calendarService;

    @Operation(summary = "일별 캘린더 일정 조회", description = """
            
            캘린더에서 날짜를 선택했을 시 해당 일자의 게시글 일정을 조회합니다.
            
            """)
    @GetMapping("/daily")
    public BaseResponse<List<ReadDailyScheduleResponse>> getDailySchedule(@RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("날짜를 입력해주세요.");
        }
        List<ReadDailyScheduleResponse> schedules = calendarService.getDailySchedule(date);
        return new BaseResponse<>(schedules);
    }

    @Operation(summary = "월별 캘린더 조회", description = """
            
            월별 캘린더를 조회하여 해당 월의 게시글 일정 수를 일자별로 한 눈에 보여줍니다.
            
            """)
    @GetMapping("/monthly")
    public BaseResponse<List<ReadMonthlyScheduleResponse>> getMonthlySchedule(@RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("날짜를 입력해주세요.");
        }
        List<ReadMonthlyScheduleResponse> schedules = calendarService.getMonthlySchedule(date);
        return new BaseResponse<>(schedules);
    }

}
