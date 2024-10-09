package RIKU.server.Dto.User;

import RIKU.server.Entity.User.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserSignUpRequestDto {
    private String loginId;
    private String password;
    private String name;
    private String major;
    private String studentId;
    private String phone;


    // Dto 객체를 Entity로 변환
    public User toEntity() {
        return new User(loginId, password, name, major, studentId, phone);
    }
}
