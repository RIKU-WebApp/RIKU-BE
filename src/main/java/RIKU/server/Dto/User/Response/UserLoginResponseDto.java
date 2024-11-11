package RIKU.server.Dto.User.Response;

import RIKU.server.Entity.User.User;
import RIKU.server.Security.JwtInfo;
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
    private String studentId;
    private JwtInfo jwtInfo;

    public static UserLoginResponseDto of (Long id, String studentId, JwtInfo jwtInfo) {
        return UserLoginResponseDto.builder()
                .userId(id)
                .studentId(studentId)
                .jwtInfo(jwtInfo)
                .build();
    }

}
