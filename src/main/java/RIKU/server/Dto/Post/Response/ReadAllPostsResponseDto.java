package RIKU.server.Dto.Post.Response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ReadAllPostsResponseDto {
    private List<ReadPostResponseDto> todayRuns; // 오늘의 러닝
    private List<ReadPostResponseDto> upcomingRuns; // 예정된 러닝
    private List<ReadPostResponseDto> pastRuns; // 지난 러닝

    public static ReadAllPostsResponseDto of (List<ReadPostResponseDto> todayRuns, List<ReadPostResponseDto> upcomingRuns, List<ReadPostResponseDto> pastRuns) {
        return ReadAllPostsResponseDto.builder()
                .todayRuns(todayRuns)
                .upcomingRuns(upcomingRuns)
                .pastRuns(pastRuns)
                .build();

    }
}
