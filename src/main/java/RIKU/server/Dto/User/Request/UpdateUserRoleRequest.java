package RIKU.server.Dto.User.Request;

import RIKU.server.Entity.User.User;
import RIKU.server.Entity.User.UserRole;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class UpdateUserRoleRequest {

    private String studentId;

    private UserRole userRole;

    public static UpdateUserRoleRequest of(User user) {
        return UpdateUserRoleRequest.builder()
                .studentId(user.getStudentId())
                .userRole(user.getUserRole())
                .build();
    }
}