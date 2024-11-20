package RIKU.server.Controller;

import RIKU.server.Dto.Calendar.Request.ScheduleRequestDto;
import RIKU.server.Dto.Calendar.Response.DailyScheduleResponseDto;
import RIKU.server.Dto.Calendar.Response.MonthlyScheduleResponseDto;
import RIKU.server.Service.CalendarService;
import RIKU.server.Util.BaseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/calendar")
public class CalendarController {

    private final CalendarService calendarService;

    // 해당 날짜 게시글 조회
    @PostMapping("/daily")
    public BaseResponse<List<DailyScheduleResponseDto>> getDailySchedule(@RequestBody ScheduleRequestDto requestDto) {
        List<DailyScheduleResponseDto> schedules = calendarService.getDailySchedule(requestDto.getDate());
        return new BaseResponse<>(schedules);
    }

    // 월별 게시글 조회
    @PostMapping("/monthly")
    public BaseResponse<List<MonthlyScheduleResponseDto>> getMonthlySchedule(@RequestBody ScheduleRequestDto requestDto) {
        List<MonthlyScheduleResponseDto> schedules = calendarService.getMonthlySchedule(requestDto.getDate());
        return new BaseResponse<>(schedules);
    }

}
