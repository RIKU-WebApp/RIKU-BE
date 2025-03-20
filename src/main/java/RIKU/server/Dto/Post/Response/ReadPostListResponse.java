package RIKU.server.Dto.Post.Response;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class ReadPostListResponse {

    private List<ReadPostPreviewResponse> todayRuns; // 오늘의 러닝

    private List<ReadPostPreviewResponse> upcomingRuns; // 예정된 러닝

    private List<ReadPostPreviewResponse> pastRuns; // 지난 러닝

    private ReadPostListResponse(List<ReadPostPreviewResponse> todayRuns, List<ReadPostPreviewResponse> upcomingRuns, List<ReadPostPreviewResponse> pastRuns) {
        this.todayRuns = todayRuns;
        this.upcomingRuns = upcomingRuns;
        this.pastRuns = pastRuns;
    }
    public static ReadPostListResponse of (List<ReadPostPreviewResponse> todayRuns, List<ReadPostPreviewResponse> upcomingRuns, List<ReadPostPreviewResponse> pastRuns) {
        return new ReadPostListResponse(todayRuns, upcomingRuns, pastRuns);
    }
}
