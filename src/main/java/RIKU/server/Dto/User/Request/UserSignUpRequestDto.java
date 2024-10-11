package RIKU.server.Dto.User.Request;

import RIKU.server.Entity.User.User;
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
public class UserSignUpRequestDto {

    @Pattern(regexp = "^([0-9]){9}$", message = "숫자 9자리")
    private String loginId;

    @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*[0-9])(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]).*$", message = "영문, 숫자, 특수문자 혼합")
    @Length(min = 8, max = 20, message = "8 ~ 20자리 이내")
    private String password;

    private String name;

    private String college;

    private String major;

    @Pattern(regexp = "^(01[016789]{1})-?[0-9]{3,4}-?[0-9]{4}$", message = "전화번호 형태")
    private String phone;


    // Dto 객체를 Entity로 변환
    public User toEntity(String encodedPassword) {
        return User.builder()
                .loginId(this.loginId)
                .password(encodedPassword)
                .name(this.name)
                .college(this.college)
                .major(this.major)
                .phone(this.phone)
                .build();
    }
}
