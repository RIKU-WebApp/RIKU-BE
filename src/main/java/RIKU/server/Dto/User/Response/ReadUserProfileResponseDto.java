package RIKU.server.Dto.User.Response;

import RIKU.server.Entity.User.User;
import RIKU.server.Entity.User.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ReadUserProfileResponseDto {
    private String studentId;
    private String userName;
    private String userProfileImg;
    private UserRole userRole;

    public static ReadUserProfileResponseDto of(User user) {
        return ReadUserProfileResponseDto.builder()
                .studentId(user.getStudentId())
                .userName(user.getName())
                .userProfileImg(user.getProfileImageUrl())
                .userRole(user.getUserRole())
                .build();
    }

}
