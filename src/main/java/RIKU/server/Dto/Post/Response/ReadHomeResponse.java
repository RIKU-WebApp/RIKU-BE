package RIKU.server.Dto.Post.Response;

import RIKU.server.Entity.User.User;
import RIKU.server.Entity.User.UserRole;
import lombok.Getter;

@Getter
public class ReadHomeResponse {

    private UserRole userRole;

    private ReadHomeCardResponse flashRun;

    private ReadHomeCardResponse regularRun;

    private ReadHomeCardResponse trainingRun;

    private ReadHomeCardResponse eventRun;


    private ReadHomeResponse (User user, ReadHomeCardResponse flashRun, ReadHomeCardResponse regularRun, ReadHomeCardResponse trainingRun, ReadHomeCardResponse eventRun) {
        this.userRole = user.getUserRole();
        this.flashRun = flashRun;
        this.regularRun = regularRun;
        this.trainingRun = trainingRun;
        this.eventRun = eventRun;
    }
    public static ReadHomeResponse of (User user, ReadHomeCardResponse flashRun, ReadHomeCardResponse regularRun, ReadHomeCardResponse trainingRun, ReadHomeCardResponse eventRun) {
        return new ReadHomeResponse(user, flashRun, regularRun, trainingRun, eventRun);
    }
}
