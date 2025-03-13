package RIKU.server.Dto.User.Request;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import org.hibernate.validator.constraints.Length;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@NoArgsConstructor
public class UpdateProfileRequest {
    @Nullable
    @Pattern(regexp = "^(01[016789]{1})-?[0-9]{3,4}-?[0-9]{4}$", message = "전화번호 형태")
    private String phone;

    @Nullable
    @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*[0-9])(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]).*$", message = "영문, 숫자, 특수문자 혼합")
    @Length(min = 8, max = 20, message = "8 ~ 20자리 이내")
    private String password;

    @Nullable
    private MultipartFile userProfileImg;
}
