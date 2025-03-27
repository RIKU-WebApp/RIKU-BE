package RIKU.server.Dto.User.Response;

import RIKU.server.Entity.User.User;
import RIKU.server.Entity.User.UserRole;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class ReadUsersResponse {

    private String studentId;

    private String userName;

    private String college;

    private String major;

    private String phone;

    private int points;

    private int participationCount;

    private UserRole userRole;

    public static ReadUsersResponse of(User user, int points, int participationCount) {
        return ReadUsersResponse.builder()
                .studentId(user.getStudentId())
                .userName(user.getName())
                .college(user.getCollege())
                .major(user.getMajor())
                .phone(user.getPhone())
                .points(points)
                .participationCount(participationCount)
                .userRole(user.getUserRole())
                .build();
    }
}
