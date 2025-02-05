package RIKU.server.Dto.User.Response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserRankingResponseDto {

    private List<UserPointResponseDto> top10;
    private int userRanking; // 사용자 순위
    private UserPointResponseDto userPoints; // 사용자 정보 및 포인트

    public static UserRankingResponseDto of(List<UserPointResponseDto> topUsers, int userRanking, UserPointResponseDto userPoints) {
        return UserRankingResponseDto.builder()
                .top10(topUsers)
                .userRanking(userRanking)
                .userPoints(userPoints)
                .build();
    }
}
