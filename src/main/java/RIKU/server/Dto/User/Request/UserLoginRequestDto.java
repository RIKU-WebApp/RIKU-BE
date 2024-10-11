package RIKU.server.Dto.User.Request;

import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserLoginRequestDto {

    @Pattern(regexp = "^([0-9]){9}$", message = "숫자 9자리")
    private String loginId;

    @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*[0-9])(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]).*$", message = "영문, 숫자, 특수문자 혼합")
    @Length(min = 8, max = 20, message = "8 ~ 20자리 이내")
    private String password;
}
