package RIKU.server.Dto.Post.Response;

import lombok.Getter;

@Getter
public class ReadHomeResponse {

    private ReadHomeCardResponse flashRun;

    private ReadHomeCardResponse regularRun;

    private ReadHomeCardResponse trainingRun;

    private ReadHomeCardResponse eventRun;


    private ReadHomeResponse (ReadHomeCardResponse flashRun, ReadHomeCardResponse regularRun, ReadHomeCardResponse trainingRun, ReadHomeCardResponse eventRun) {
        this.flashRun = flashRun;
        this.regularRun = regularRun;
        this.trainingRun = trainingRun;
        this.eventRun = eventRun;
    }
    public static ReadHomeResponse of (ReadHomeCardResponse flashRun, ReadHomeCardResponse regularRun, ReadHomeCardResponse trainingRun, ReadHomeCardResponse eventRun) {
        return new ReadHomeResponse(flashRun, regularRun, trainingRun, eventRun);
    }
}
