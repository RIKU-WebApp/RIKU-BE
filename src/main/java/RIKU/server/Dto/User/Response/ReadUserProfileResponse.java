package RIKU.server.Dto.User.Response;

import RIKU.server.Entity.User.User;
import RIKU.server.Entity.User.UserRole;
import lombok.Getter;

@Getter
public class ReadUserProfileResponse {

    private String studentId;

    private String userName;

    private String userProfileImgUrl;

    private UserRole userRole;

    private int points;

    private int attendanceCount;

    private ReadUserProfileResponse(String studentId, String userName, String userProfileImgUrl, UserRole userRole, int points, int attendanceCount) {
        this.studentId = studentId;
        this.userName = userName;
        this.userProfileImgUrl = userProfileImgUrl;
        this.userRole = userRole;
        this.points = points;
        this.attendanceCount = attendanceCount;
    }

    public static ReadUserProfileResponse of(User user, int points, int attendanceCount) {
        return new ReadUserProfileResponse(
                user.getStudentId(),
                user.getName(),
                user.getProfileImageUrl(),
                user.getUserRole(),
                points,
                attendanceCount);
    }
}
