package RIKU.server.Service;

import RIKU.server.Dto.User.Response.ReadUserPointResponse;
import RIKU.server.Dto.User.Response.ReadUserRankingResponse;
import RIKU.server.Entity.Base.BaseStatus;
import RIKU.server.Entity.User.User;
import RIKU.server.Entity.User.UserPoint;
import RIKU.server.Repository.UserPointRepository;
import RIKU.server.Repository.UserRepository;
import RIKU.server.Security.AuthMember;
import RIKU.server.Util.BaseResponseStatus;
import RIKU.server.Util.Exception.Domain.UserException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserPointService {

    private final UserRepository userRepository;
    private final UserPointRepository userPointRepository;

    public ReadUserRankingResponse getUserPointRanking(AuthMember authMember) {
        // 1. 유저 조회
        User user = userRepository.findById(authMember.getId())
                .orElseThrow(() -> new UserException(BaseResponseStatus.USER_NOT_FOUND));

        // 2. 전체 사용자 조회
        List<User> users = userRepository.findByStatus(BaseStatus.ACTIVE);

        // 3. 유저별 포인트 총합 계산 후 정렬
        List<ReadUserPointResponse> userPointList = users.stream()
                .map(u -> ReadUserPointResponse.of(u, userPointRepository.sumPointsByUser(u)))
                .sorted(Comparator.comparingInt(ReadUserPointResponse::getTotalPoints).reversed()
                                .thenComparing(ReadUserPointResponse::getUserName))
                .toList();

        // 4. 공동 순위 처리
        int rank = 1;      // 현재 순위
        int prevPoints = -1;  // 이전 유저의 포인트 값
        int actualRank = 1;   // 공동 순위 조정
        int userRanking = -1;   // 현재 로그인한 유저 랭킹 저장

        for (int i = 0; i < userPointList.size(); i++) {
            ReadUserPointResponse response = userPointList.get(i);
            if (response.getTotalPoints() != prevPoints) {
                actualRank = rank;
            }
            if (response.getUserId().equals(user.getId())) {
                userRanking = actualRank;
            }
            prevPoints = response.getTotalPoints();
            rank++;
        }


        // 5. TOP 10 반환
        List<ReadUserPointResponse> top10 = userPointList.stream()
                .limit(10)
                .toList();

        ReadUserPointResponse currentUserPoint = userPointList.stream()
                .filter(p -> p.getUserId().equals(user.getId()))
                .findFirst()
                .orElse(ReadUserPointResponse.of(user, 0)); // 못 찾은 경우 대비

        return ReadUserRankingResponse.of(top10, userRanking, currentUserPoint);
    }
}
