package RIKU.server.domains.point.dto.response;

import RIKU.server.domains.user.entity.User;
import lombok.Getter;

@Getter
public class ReadUserPointResponse {

    private Long userId;

    private String userProfileImg;

    private String userName;

    private int totalPoints;

    private ReadUserPointResponse(Long userId, String userProfileImg, String userName, int totalPoints) {
        this.userId = userId;
        this.userProfileImg = userProfileImg;
        this.userName = userName;
        this.totalPoints = totalPoints;
    }

    public static ReadUserPointResponse of(User user, int totalPoints) {
        return new ReadUserPointResponse(
                user.getId(),
                user.getProfileImageUrl(),
                user.getName(),
                totalPoints
        );
    }
}
