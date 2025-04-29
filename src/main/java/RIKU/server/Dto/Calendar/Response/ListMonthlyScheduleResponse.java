package RIKU.server.Dto.Calendar.Response;

import RIKU.server.Entity.Base.BaseStatus;
import RIKU.server.Entity.User.User;
import RIKU.server.Entity.User.UserRole;
import lombok.Getter;

import java.util.List;

@Getter
public class ListMonthlyScheduleResponse {

    private String userRole;

    private List<ReadMonthlyScheduleResponse> schedules;

    private ListMonthlyScheduleResponse(User user, List<ReadMonthlyScheduleResponse> schedules) {
        this.userRole = toUserRole(user);
        this.schedules = schedules;
    }

    public static ListMonthlyScheduleResponse of(User user, List<ReadMonthlyScheduleResponse> schedules) {
        return new ListMonthlyScheduleResponse(user, schedules);
    }

    private static String toUserRole(User user) {
        if (user.getUserRole() == UserRole.ADMIN) return "ADMIN";
        if (user.getUserRole() == UserRole.NEW_MEMBER) return "NEW_MEMBER";
        if (user.getUserRole() == UserRole.MEMBER) {
            if (Boolean.TRUE.equals(user.getIsPacer())) return "PACER";
            return "MEMBER";
        }
        if (user.getStatus() == BaseStatus.INACTIVE) return "INACTIVE";
        return "UNKNOWN";
    }
}
