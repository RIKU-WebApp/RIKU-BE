package RIKU.server.Dto.User.Response;

import RIKU.server.Entity.User.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserSignUpResponseDto {
    private Long userId;
    private String studentId;
    private String name;

    public static UserSignUpResponseDto of (User user) {
        return UserSignUpResponseDto.builder()
                .userId(user.getId())
                .studentId(user.getStudentId())
                .name(user.getName())
                .build();
    }
}
