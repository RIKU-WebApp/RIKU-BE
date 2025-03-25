package RIKU.server.Dto.User.Response;

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

    private UserRole userRole;

    private int points;

    private int participationCount;

    private List<LocalDate> profileAttendanceDates;

    private ReadUserProfileResponse(String studentId, String userName, String userProfileImgUrl, UserRole userRole, int points, int participationCount, List<LocalDate> profileAttendanceDates) {
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
                user.getUserRole(),
                points,
                participationCount,
                profileAttendanceDates);
    }
}
