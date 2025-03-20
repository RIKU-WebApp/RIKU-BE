package RIKU.server.Dto.User.Request;

import RIKU.server.Entity.User.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

@Getter
@NoArgsConstructor
public class SignUpUserRequest {

    @Pattern(regexp = "^([0-9]){9}$", message = "숫자 9자리")
    @NotBlank(message = "학번은 필수 항목입니다.")
    private String studentId;

    @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*[0-9])(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]).*$", message = "영문, 숫자, 특수문자 혼합")
    @Length(min = 8, max = 20, message = "8 ~ 20자리 이내")
    @NotBlank(message = "비밀번호는 필수 항목입니다.")
    private String password;

    @NotBlank(message = "이름은 필수 항목입니다.")
    private String name;

    @NotBlank(message = "대학은 필수 항목입니다.")
    private String college;

    @NotBlank(message = "학과는 필수 항목입니다.")
    private String major;

    @Pattern(regexp = "^(01[016789]{1})-?[0-9]{3,4}-?[0-9]{4}$", message = "전화번호 형태")
    private String phone;

    public User toEntity(SignUpUserRequest request, String encodedPassword) {
        return User.create(
                request.getStudentId(),
                encodedPassword,
                request.getName(),
                request.getCollege(),
                request.getMajor(),
                request.getPhone());
    }
}
