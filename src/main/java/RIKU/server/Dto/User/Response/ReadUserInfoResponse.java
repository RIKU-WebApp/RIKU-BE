package RIKU.server.Dto.User.Response;

import RIKU.server.Entity.User.User;
import RIKU.server.Entity.User.UserRole;
import lombok.Getter;

@Getter
public class ReadUserInfoResponse {

    private Long userId;

    private String userProfileImg;

    private String userName;

    private String userRole;

    private ReadUserInfoResponse(Long userId, String userProfileImg, String userName, UserRole userRole) {
        this.userId = userId;
        this.userProfileImg = userProfileImg;
        this.userName = userName;
        this.userRole = String.valueOf(userRole);
    }

    public static ReadUserInfoResponse of(User user) {
        return new ReadUserInfoResponse(user.getId(), user.getProfileImageUrl(), user.getName(), user.getUserRole());
    }
}
