package RIKU.server.domains.point.service;

import RIKU.server.domains.point.dto.response.ReadPointListResponse;
import RIKU.server.domains.point.dto.response.ReadUserParticipationsResponse;
import RIKU.server.domains.point.entity.PointType;
import RIKU.server.domains.point.repository.UserPointRepository;
import RIKU.server.domains.run.participant.entity.ParticipantStatus;
import RIKU.server.domains.run.participant.repository.ParticipantRepository;
import RIKU.server.domains.user.exception.UserException;
import RIKU.server.domains.user.repository.UserRepository;
import RIKU.server.global.response.BaseResponseStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserActivityService {
    private final UserRepository userRepository;
    private final UserPointRepository userPointRepository;
    private final ParticipantRepository participantRepository;

    @Transactional(readOnly = true)
    public ReadUserParticipationsResponse getParticipations(Long userId, LocalDateTime fromUtc, int rank) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(BaseResponseStatus.USER_NOT_FOUND));
        var points = userPointRepository.findByUserAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(user, fromUtc).stream()
                .map(point -> ReadPointListResponse.of(point, tag(point.getPointType()))).toList();
        int total = userPointRepository.sumPointsByUserSince(user, fromUtc);
        int attendance = participantRepository.countByUserAndParticipantStatusAndCreatedAtGreaterThanEqual(
                user, ParticipantStatus.ATTENDED, fromUtc);
        return ReadUserParticipationsResponse.of(user, total, attendance, rank, points);
    }

    private String tag(PointType type) {
        return switch (type) {
            case ADD_REGULAR_JOIN -> "정규런";
            case ADD_FLASH_CREATE, ADD_FLASH_JOIN -> "번개런";
            case ADD_TRAINING_JOIN -> "훈련";
            case ADD_EVENT_JOIN -> "행사";
            case ADD_ATTENDANCE -> "출석";
            case REMOVE -> "차감";
        };
    }
}
