package RIKU.server.Dto.User.Request;

import RIKU.server.Entity.User.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserRoleRequestDto {
    private UserRole userRole;
}
