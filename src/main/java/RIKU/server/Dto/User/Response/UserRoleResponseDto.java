package RIKU.server.Dto.User.Response;

import RIKU.server.Entity.User.UserRole;
import RIKU.server.Security.JwtInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserRoleResponseDto {
    private Long userId;
    private UserRole userRole;
    private JwtInfo jwtInfo;
}
