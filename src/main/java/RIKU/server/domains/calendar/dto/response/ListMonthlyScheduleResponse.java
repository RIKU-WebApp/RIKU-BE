package RIKU.server.domains.calendar.dto.response;

import RIKU.server.global.entity.BaseStatus;
import RIKU.server.domains.user.entity.User;
import RIKU.server.domains.user.entity.UserRole;
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
