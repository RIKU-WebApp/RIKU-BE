package RIKU.server.Dto.User.Response;

import lombok.Getter;

import java.util.List;
@Getter
public class ReadUserRankingResponse {

    private List<ReadUserPointResponse> top10;

    private int userRanking; // 사용자 순위

    private ReadUserPointResponse userPoints; // 사용자 정보 및 포인트

    private ReadUserRankingResponse(List<ReadUserPointResponse> topUsers, int userRanking, ReadUserPointResponse userPoints) {
        this.top10 = topUsers;
        this.userRanking = userRanking;
        this.userPoints = userPoints;
    }

    public static ReadUserRankingResponse of(List<ReadUserPointResponse> topUsers, int userRanking, ReadUserPointResponse userPoints) {
        return new ReadUserRankingResponse(topUsers, userRanking, userPoints);
    }
}
