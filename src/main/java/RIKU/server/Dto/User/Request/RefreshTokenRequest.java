package RIKU.server.Dto.User.Request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class RefreshTokenRequest {

    @NotBlank
    private String refreshToken;
}
