package RIKU.server.Dto.User.Response;

import RIKU.server.Security.JwtInfo;
import lombok.Getter;

@Getter
public class LoginUserResponse {

    private Long userId;

    private String studentId;

    private JwtInfo jwtInfo;

    private LoginUserResponse(Long userId, String studentId, JwtInfo jwtInfo) {
        this.userId = userId;
        this.studentId = studentId;
        this.jwtInfo = jwtInfo;
    }
    public static LoginUserResponse of (Long id, String studentId, JwtInfo jwtInfo) {
        return new LoginUserResponse(id, studentId, jwtInfo);
    }
}
