package RIKU.server.Dto.Post.Response;

import lombok.Getter;

@Getter
public class ReadHomeResponse {

    private ReadHomeCardResponse regularRun;

    private ReadHomeCardResponse flashRun;

    private ReadHomeCardResponse trainingRun;

    private ReadHomeCardResponse eventRun;


    private ReadHomeResponse (ReadHomeCardResponse regularRun, ReadHomeCardResponse flashRun, ReadHomeCardResponse trainingRun, ReadHomeCardResponse eventRun) {
        this.regularRun = regularRun;
        this.flashRun = flashRun;
        this.trainingRun = trainingRun;
        this.eventRun = eventRun;
    }
    public static ReadHomeResponse of (ReadHomeCardResponse regularRun, ReadHomeCardResponse flashRun, ReadHomeCardResponse trainingRun, ReadHomeCardResponse eventRun) {
        return new ReadHomeResponse(regularRun, flashRun, trainingRun, eventRun);
    }
}
