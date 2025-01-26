package RIKU.server.Dto.User.Response;

import RIKU.server.Entity.User.User;
import RIKU.server.Entity.User.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ReadUsersResponseDto {
    private String studentId;
    private String userName;
    private String college;
    private String major;
    private String phone;
    private int point;
    private int attendanceCount;
    private UserRole userRole;

    public static ReadUsersResponseDto of(User user) {
        return ReadUsersResponseDto.builder()
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
