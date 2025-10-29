package RIKU.server.Service;

import RIKU.server.Config.SeasonProvider;
import RIKU.server.Dto.User.Request.ReadUserEventRankingRequest;
import RIKU.server.Dto.User.Response.ReadPointListResponse;
import RIKU.server.Dto.User.Response.ReadUserParticipationsResponse;
import RIKU.server.Dto.User.Response.ReadUserPointResponse;
import RIKU.server.Dto.User.Response.ReadUserRankingResponse;
import RIKU.server.Entity.Base.BaseStatus;
import RIKU.server.Entity.Participant.ParticipantStatus;
import RIKU.server.Entity.User.PointType;
import RIKU.server.Entity.User.User;
import RIKU.server.Entity.User.UserPoint;
import RIKU.server.Repository.ParticipantRepository;
import RIKU.server.Repository.UserPointRepository;
import RIKU.server.Repository.UserRepository;
import RIKU.server.Security.AuthMember;
import RIKU.server.Util.BaseResponseStatus;
import RIKU.server.Util.DateTimeUtils;
import RIKU.server.Util.Exception.Domain.UserException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserPointService {

    private final UserRepository userRepository;
    private final UserPointRepository userPointRepository;
    private final ParticipantRepository participantRepository;
    private final SeasonProvider seasonProvider;

    // 전체 랭킹(시즌)
    public ReadUserRankingResponse getUserPointRanking(AuthMember authMember) {
        // 1. 유저 조회
        User user = userRepository.findById(authMember.getId())
                .orElseThrow(() -> new UserException(BaseResponseStatus.USER_NOT_FOUND));

        return calculateUserRankAndTop20(user, seasonProvider.seasonStartUtc());
    }

    // 마이페이지 활동 내역 조회
    public ReadUserParticipationsResponse getParticipations(AuthMember authMember) {
        // 1. 유저 조회
        User user = userRepository.findById(authMember.getId())
                .orElseThrow(() -> new UserException(BaseResponseStatus.USER_NOT_FOUND));

        LocalDateTime fromUtc = seasonProvider.seasonStartUtc();

        // 2. 유저 포인트 조회
        List<UserPoint> userPoints = userPointRepository.findByUserAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(user, fromUtc);

        // 3. 포인트 총합 계산
        int totalPoint = userPointRepository.sumPointsByUserSince(user, fromUtc);

        // 4. 참여 내역 수
        int participationCount = participantRepository.countByUserAndParticipantStatusAndCreatedAtGreaterThanEqual(user, ParticipantStatus.ATTENDED, fromUtc);

        // 5. 랭킹 계산
        ReadUserRankingResponse ranking = calculateUserRankAndTop20(user, fromUtc);

        // 6. 포인트 리스트 DTO 변환
        List<ReadPointListResponse> pointResponses = userPoints.stream()
                .map(up -> ReadPointListResponse.of(up, mapPointTypeToTag(up.getPointType())))
                .toList();

        return ReadUserParticipationsResponse.of(user, totalPoint, participationCount, ranking.getUserRanking(), pointResponses);
    }

    // 이벤트 랭킹 페이지 조회
    public ReadUserRankingResponse getUserEventPointRanking(ReadUserEventRankingRequest request, AuthMember authMember) {
        // 1. 유저 조회
        User user = userRepository.findById(authMember.getId())
                .orElseThrow(() -> new UserException(BaseResponseStatus.USER_NOT_FOUND));

        // 2. 전체 사용자 조회
        List<User> users = userRepository.findByStatus(BaseStatus.ACTIVE);

        // 3. utc 시간 변경
        LocalDateTime startUtc = DateTimeUtils.startOfDayUtc(request.getStartDate());
        LocalDateTime endUtc = DateTimeUtils.endOfDayUtc(request.getEndDate());
        log.info("이벤트 랭킹 조회 범위: {} ~ {} (KST 기준), 변환된 UTC: {} ~ {}",
                request.getStartDate(), request.getEndDate(), startUtc, endUtc);

        // 4. 유저별 포인트 총합 계산 후 정렬
        List<ReadUserPointResponse> userPointList = users.stream()
                .map(u -> {
                    int total = userPointRepository
                            .findByUserAndCreatedAtBetween(u, startUtc, endUtc)
                            .stream()
                            .filter(p -> request.getPointTypes().contains(p.getPointType()))
                            .mapToInt(UserPoint::getPoint)
                            .sum();
                    return ReadUserPointResponse.of(u, total);
                        } )
                .sorted(Comparator.comparingInt(ReadUserPointResponse::getTotalPoints).reversed()
                        .thenComparing(ReadUserPointResponse::getUserName))
                .toList();
        return processRanking(userPointList, authMember.getId());
    }

    private ReadUserRankingResponse calculateUserRankAndTop20(User user, LocalDateTime fromUtc) {

        // 전체 사용자 조회
        List<User> users = userRepository.findByStatus(BaseStatus.ACTIVE);

        // 유저별 포인트 총합 계산 후 정렬
        List<ReadUserPointResponse> userPointList = users.stream()
                .map(u -> ReadUserPointResponse.of(u, userPointRepository.sumPointsByUserSince(u, fromUtc)))
                .sorted(Comparator.comparingInt(ReadUserPointResponse::getTotalPoints).reversed()
                        .thenComparing(ReadUserPointResponse::getUserName))
                .toList();

        return processRanking(userPointList, user.getId());
    }

    private ReadUserRankingResponse processRanking(List<ReadUserPointResponse> userPointList, Long userId) {
        // 공동 순위 처리
        int rank = 1;      // 현재 순위
        int prevPoints = -1;  // 이전 유저의 포인트 값
        int actualRank = 1;   // 공동 순위 조정
        int userRanking = -1;   // 현재 로그인한 유저 랭킹 저장

        for (ReadUserPointResponse r : userPointList) {
            if (r.getTotalPoints() != prevPoints) {
                actualRank = rank;
            }
            if (r.getUserId().equals(userId)) {
                userRanking = actualRank;
            }
            prevPoints = r.getTotalPoints();
            rank++;
        }

        // TOP 20 반환
        List<ReadUserPointResponse> top20 = userPointList.stream()
                .limit(20)
                .toList();

        ReadUserPointResponse currentUserPoint = userPointList.stream()
                .filter(p -> p.getUserId().equals(userId))
                .findFirst()
                .orElse(null); // 못 찾은 경우 대비

        return ReadUserRankingResponse.of(top20, userRanking, currentUserPoint);
    }

    private String mapPointTypeToTag(PointType pointType) {
        return switch (pointType) {
            case ADD_REGULAR_JOIN -> "정규런";
            case ADD_FLASH_CREATE, ADD_FLASH_JOIN -> "번개런";
            case ADD_TRAINING_JOIN -> "훈련";
            case ADD_EVENT_JOIN -> "행사";
            case ADD_ATTENDANCE -> "출석";
            case REMOVE -> "차감";
            default -> "기타";
        };
    }
}
