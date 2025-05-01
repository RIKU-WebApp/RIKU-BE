package RIKU.server.Service;

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

    public ReadUserRankingResponse getUserPointRanking(AuthMember authMember) {
        // 1. 유저 조회
        User user = userRepository.findById(authMember.getId())
                .orElseThrow(() -> new UserException(BaseResponseStatus.USER_NOT_FOUND));

        return calculateUserRankAndTop20(user);
    }

    // 활동 내역 조회
    public ReadUserParticipationsResponse getParticipations(AuthMember authMember) {
        // 1. 유저 조회
        User user = userRepository.findById(authMember.getId())
                .orElseThrow(() -> new UserException(BaseResponseStatus.USER_NOT_FOUND));

        // 2. 유저 포인트 조회
        List<UserPoint> userPoints = userPointRepository.findAll().stream()
                .filter(p -> p.getUser().equals(user))
                .sorted(Comparator.comparing(UserPoint::getCreatedAt).reversed())
                .toList();

        // 3. 포인트 총합 계산
        int totalPoint = userPointRepository.sumPointsByUser(user);

        // 🔍 포인트별 createdAt 로그 출력
        log.info("📌 유저 ID={}의 포인트 총합: {}", user.getId(), totalPoint);

        userPoints.forEach(point -> {
            var utcTime = point.getCreatedAt();
            var kstDate = DateTimeUtils.toUserLocalDate(utcTime);
            log.info("🟡 Point ID: {}, UTC createdAt: {}, KST LocalDate: {}, Type: {}",
                    point.getId(), utcTime, kstDate, point.getPointType());
        });

        // 4. 참여 내역 수
        int participationCount = participantRepository.countByUserAndParticipantStatus(user, ParticipantStatus.ATTENDED);

        // 5. 랭킹 계산
        ReadUserRankingResponse ranking = calculateUserRankAndTop20(user);

        // 6. 포인트 리스트 DTO 변환
        List<ReadPointListResponse> pointResponses = userPoints.stream()
                .map(up -> ReadPointListResponse.of(up, mapPointTypeToTag(up.getPointType())))
                .toList();

        return ReadUserParticipationsResponse.of(user, totalPoint, participationCount, ranking.getUserRanking(), pointResponses);
    }

    private ReadUserRankingResponse calculateUserRankAndTop20(User user) {

        // 전체 사용자 조회
        List<User> users = userRepository.findByStatus(BaseStatus.ACTIVE);

        // 유저별 포인트 총합 계산 후 정렬
        List<ReadUserPointResponse> userPointList = users.stream()
                .map(u -> ReadUserPointResponse.of(u, userPointRepository.sumPointsByUser(u)))
                .sorted(Comparator.comparingInt(ReadUserPointResponse::getTotalPoints).reversed()
                        .thenComparing(ReadUserPointResponse::getUserName))
                .toList();

        // 공동 순위 처리
        int rank = 1;      // 현재 순위
        int prevPoints = -1;  // 이전 유저의 포인트 값
        int actualRank = 1;   // 공동 순위 조정
        int userRanking = -1;   // 현재 로그인한 유저 랭킹 저장

        for (ReadUserPointResponse r : userPointList) {
            if (r.getTotalPoints() != prevPoints) {
                actualRank = rank;
            }
            if (r.getUserId().equals(user.getId())) {
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
                .filter(p -> p.getUserId().equals(user.getId()))
                .findFirst()
                .orElse(ReadUserPointResponse.of(user, 0)); // 못 찾은 경우 대비

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
