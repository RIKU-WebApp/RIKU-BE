package RIKU.server.Service;

import RIKU.server.Dto.User.Response.UserPointResponseDto;
import RIKU.server.Dto.User.Response.UserRankingResponseDto;
import RIKU.server.Entity.User.User;
import RIKU.server.Repository.UserRepository;
import RIKU.server.Util.BaseResponseStatus;
import RIKU.server.Util.Exception.Domain.UserException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserPointService {
    private final UserRepository userRepository;

    public UserRankingResponseDto getUserPointRanking(Long userId) {
        // Top 10 유저 조회
        List<UserPointResponseDto> topUsers = userRepository.findTop10ByOrderByTotalPointsDescNameAsc()
                .stream()
                .map(UserPointResponseDto::of)
                .toList();

        // 사용자 랭킹 조회
        int userRanking = findUserRanking(userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(BaseResponseStatus.USER_NOT_FOUND));

        UserPointResponseDto userPoints = UserPointResponseDto.of(user);

        return UserRankingResponseDto.of(topUsers, userRanking, userPoints);

    }

    private int findUserRanking(Long userId) {
        List<User> rankedUsers = userRepository.findAllByOrderByTotalPointsDescNameAsc();

        int rank = 1;      // 현재 순위
        int prevPoints = -1;  // 이전 유저의 포인트 값
        int actualRank = 1;   // 공동 순위 조정

        for (User user : rankedUsers) {
            if (prevPoints != user.getTotalPoints()) {
                actualRank = rank; // 포인트가 바뀌면 순위 업데이트
            }

            if (user.getId().equals(userId)) {
                return actualRank; // 현재 사용자의 순위 반환
            }

            prevPoints = user.getTotalPoints();
            rank++;  // 전체 순위 증가
        }
        return -1;
    }
}
