package RIKU.server.Dto.User.Response;

import RIKU.server.Dto.User.Request.UserLoginRequestDto;
import RIKU.server.Entity.User.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserLoginResponseDto {

    private Long userId;
    private String loginId;
    private String name;

    public static UserLoginResponseDto of (User user) {
        return UserLoginResponseDto.builder()
                .userId(user.getId())
                .loginId(user.getLoginId())
                .name(user.getName())
                .build();
    }

}
