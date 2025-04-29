package RIKU.server.Dto.User.Response;

import RIKU.server.Entity.Base.BaseStatus;
import RIKU.server.Entity.User.User;
import RIKU.server.Entity.User.UserRole;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
public class ReadUserProfileResponse {

    private String studentId;

    private String userName;

    private String userProfileImgUrl;

    private String userRole;

    private int points;

    private int participationCount;

    private List<LocalDate> profileAttendanceDates;

    private ReadUserProfileResponse(String studentId, String userName, String userProfileImgUrl, String userRole, int points, int participationCount, List<LocalDate> profileAttendanceDates) {
        this.studentId = studentId;
        this.userName = userName;
        this.userProfileImgUrl = userProfileImgUrl;
        this.userRole = userRole;
        this.points = points;
        this.participationCount = participationCount;
        this.profileAttendanceDates = profileAttendanceDates;
    }

    public static ReadUserProfileResponse of(User user, int points, int participationCount, List<LocalDate> profileAttendanceDates) {
        return new ReadUserProfileResponse(
                user.getStudentId(),
                user.getName(),
                user.getProfileImageUrl(),
                toUserRole(user),
                points,
                participationCount,
                profileAttendanceDates);
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
