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
        List<UserPointResponseDto> topUsers = userRepository.findTop10ByOrderByTotalPointsDesc()
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
        List<User> rankedUsers = userRepository.findAllByOrderByTotalPointsDesc();

        for (int i = 0; i < rankedUsers.size(); i++) {
            if (rankedUsers.get(i).getId().equals(userId)) {
                return i + 1;
            }
        }
        return -1;
    }
}
