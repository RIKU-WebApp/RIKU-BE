package RIKU.server.domains.point.dto.response;

import RIKU.server.domains.user.entity.User;
import RIKU.server.domains.user.entity.UserRole;
import lombok.Getter;

import java.util.List;

@Getter
public class ReadUserParticipationsResponse {

    private String userProfileImg;

    private String userName;

    private UserRole userRole;

    private int totalPoint;

    private int participationCount;

    private int rank;

    private List<ReadPointListResponse> points;

    private ReadUserParticipationsResponse(String userProfileImg, String userName, UserRole userRole, int totalPoint, int participationCount, int rank, List<ReadPointListResponse> points) {
        this.userProfileImg = userProfileImg;
        this.userName = userName;
        this.userRole = userRole;
        this.totalPoint = totalPoint;
        this.participationCount = participationCount;
        this.rank = rank;
        this.points = points;
    }

    public static ReadUserParticipationsResponse of(User user, int totalPoint, int participationCount, int rank, List<ReadPointListResponse> point) {
        return new ReadUserParticipationsResponse(
                user.getProfileImageUrl(),
                user.getName(),
                user.getUserRole(),
                totalPoint,
                participationCount,
                rank,
                point
        );
    }
}
