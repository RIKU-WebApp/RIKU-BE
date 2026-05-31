package RIKU.server.domains.auth.dto.response;

import RIKU.server.domains.user.entity.UserRole;
import RIKU.server.security.JwtInfo;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
public class UpdateUserRoleResponse {

    private Long userId;

    private UserRole userRole;

    private JwtInfo jwtInfo;

    private UpdateUserRoleResponse(Long userId, UserRole userRole, JwtInfo jwtInfo) {
        this.userId = userId;
        this.userRole = userRole;
        this.jwtInfo = jwtInfo;
    }

    public static UpdateUserRoleResponse of(Long userId, UserRole userRole, JwtInfo jwtInfo) {
        return new UpdateUserRoleResponse(userId, userRole, jwtInfo);
    }
}
