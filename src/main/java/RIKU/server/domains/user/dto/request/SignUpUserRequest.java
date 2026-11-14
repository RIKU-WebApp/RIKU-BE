package RIKU.server.domains.user.dto.request;

import RIKU.server.domains.user.entity.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

@Getter
@NoArgsConstructor
public class SignUpUserRequest {

    @NotBlank(message = "학번은 필수 항목입니다.")
    @Length(max = 255, message = "학번은 255자 이내로 입력해 주세요.")
    private String studentId;

    @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*[0-9])(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]).*$", message = "영문, 숫자, 특수문자 혼합")
    @Length(min = 8, max = 20, message = "8 ~ 20자리 이내")
    @NotBlank(message = "비밀번호는 필수 항목입니다.")
    private String password;

    @NotBlank(message = "이름은 필수 항목입니다.")
    @Length(max = 255, message = "이름은 255자 이내로 입력해 주세요.")
    private String name;

    @NotBlank(message = "대학은 필수 항목입니다.")
    @Length(max = 255, message = "단과대학은 255자 이내로 입력해 주세요.")
    private String college;

    @NotBlank(message = "학과는 필수 항목입니다.")
    @Length(max = 255, message = "학과는 255자 이내로 입력해 주세요.")
    private String major;

    @Pattern(regexp = "^(01[016789]{1})-?[0-9]{3,4}-?[0-9]{4}$", message = "휴대전화 번호를 010-1234-5678 형식으로 입력하거나 비워 두세요.")
    private String phone;

    public User toEntity(SignUpUserRequest request, String encodedPassword) {
        return User.create(
                request.getStudentId(),
                encodedPassword,
                request.getName(),
                request.getCollege(),
                request.getMajor(),
                request.getPhone() == null ? null : request.getPhone().replace("-", ""));
    }
}
