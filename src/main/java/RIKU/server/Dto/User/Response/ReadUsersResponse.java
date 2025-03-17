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

    private int point;

    private int attendanceCount;

    private UserRole userRole;

    public static ReadUsersResponse of(User user) {
        return ReadUsersResponse.builder()
                .studentId(user.getStudentId())
                .userName(user.getName())
                .college(user.getCollege())
                .major(user.getMajor())
                .phone(user.getPhone())
                .point(0)
                .attendanceCount(0)
                .userRole(user.getUserRole())
                .build();
    }
}
