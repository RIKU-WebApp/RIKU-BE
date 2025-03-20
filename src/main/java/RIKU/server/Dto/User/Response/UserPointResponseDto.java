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
public class UserPointResponseDto {
    private Long userId;
    private String userProfileImg;
    private String userName;
    private int totalPoints;

    public static UserPointResponseDto of(User user) {
        return UserPointResponseDto.builder()
                .userId(user.getId())
                .userProfileImg(user.getProfileImageUrl())
                .userName(user.getName())
                .totalPoints(0)
                .build();

    }

}
