package RIKU.server.Dto.User.Request;

import RIKU.server.Entity.User.UserRole;import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UpdateUserRoleRequest {

    private String studentId;

    private UserRole userRole;
}