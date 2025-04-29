package RIKU.server.Dto.Post.Response;

import RIKU.server.Entity.Base.BaseStatus;
import RIKU.server.Entity.User.User;
import RIKU.server.Entity.User.UserRole;
import lombok.Getter;

@Getter
public class ReadHomeResponse {

    private String userRole;

    private ReadHomeCardResponse flashRun;

    private ReadHomeCardResponse regularRun;

    private ReadHomeCardResponse trainingRun;

    private ReadHomeCardResponse eventRun;


    private ReadHomeResponse (User user, ReadHomeCardResponse flashRun, ReadHomeCardResponse regularRun, ReadHomeCardResponse trainingRun, ReadHomeCardResponse eventRun) {
        this.userRole = toUserRole(user);
        this.flashRun = flashRun;
        this.regularRun = regularRun;
        this.trainingRun = trainingRun;
        this.eventRun = eventRun;
    }
    public static ReadHomeResponse of (User user, ReadHomeCardResponse flashRun, ReadHomeCardResponse regularRun, ReadHomeCardResponse trainingRun, ReadHomeCardResponse eventRun) {
        return new ReadHomeResponse(user, flashRun, regularRun, trainingRun, eventRun);
    }

    private static String toUserRole(User user) {
        if (user.getUserRole() == UserRole.ADMIN) return "ADMIN";
        if (user.getUserRole() == UserRole.NEW_MEMBER) return "NEW_MEMBER";
        if (user.getUserRole() == UserRole.MEMBER) {
            if (Boolean.TRUE.equals(user.getIsPacer())) return "PACER";
            return "MEMBER";
        }
        if (user.getStatus() == BaseStatus.INACTIVE) return "INACTIVE";
        return "UNKNOWN";
    }
}
