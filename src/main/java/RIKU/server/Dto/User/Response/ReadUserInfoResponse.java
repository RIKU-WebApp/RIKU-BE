package RIKU.server.Dto.User.Response;

import RIKU.server.Entity.User.User;
import lombok.Getter;

@Getter
public class ReadUserInfoResponse {

    private Long userId;

    private String userProfileImg;

    private String userName;

    private ReadUserInfoResponse(Long userId, String userProfileImg, String userName) {
        this.userId = userId;
        this.userProfileImg = userProfileImg;
        this.userName = userName;
    }

    public static ReadUserInfoResponse of(User user) {
        return new ReadUserInfoResponse(user.getId(), user.getProfileImageUrl(), user.getName());
    }
}
