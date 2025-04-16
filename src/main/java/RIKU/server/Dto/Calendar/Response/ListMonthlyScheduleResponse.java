package RIKU.server.Dto.Calendar.Response;

import RIKU.server.Entity.User.User;
import RIKU.server.Entity.User.UserRole;
import lombok.Getter;

import java.util.List;

@Getter
public class ListMonthlyScheduleResponse {

    private UserRole userRole;

    private List<ReadMonthlyScheduleResponse> schedules;

    private ListMonthlyScheduleResponse(User user, List<ReadMonthlyScheduleResponse> schedules) {
        this.userRole = user.getUserRole();
        this.schedules = schedules;
    }

    public static ListMonthlyScheduleResponse of(User user, List<ReadMonthlyScheduleResponse> schedules) {
        return new ListMonthlyScheduleResponse(user, schedules);
    }
}
