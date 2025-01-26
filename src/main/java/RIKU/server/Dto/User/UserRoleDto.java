package RIKU.server.Dto.User;

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
public class UserRoleDto {
    private String studentId;
    private UserRole userRole;

    public static UserRoleDto of(User user) {
        return UserRoleDto.builder()
                .studentId(user.getStudentId())
                .userRole(user.getUserRole())
                .build();
    }
}