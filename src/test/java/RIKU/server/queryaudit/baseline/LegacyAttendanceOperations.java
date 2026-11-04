package RIKU.server.queryaudit.baseline;

import RIKU.server.domains.point.ranking.RankingChanged;
import org.springframework.context.ApplicationEventPublisher;
import RIKU.server.domains.run.participant.dto.request.ManualAttendParticipantRequest;
import RIKU.server.domains.run.participant.dto.response.UpdateParticipantResponse;
import RIKU.server.domains.run.pacer.entity.Pacer;
import RIKU.server.domains.run.post.entity.*;
import RIKU.server.domains.run.post.entity.PostStatus;
import RIKU.server.domains.run.participant.entity.Participant;
import RIKU.server.domains.run.participant.entity.ParticipantStatus;
import RIKU.server.domains.point.entity.PointType;
import RIKU.server.domains.user.entity.User;
import RIKU.server.domains.point.entity.UserPoint;
import RIKU.server.domains.run.post.repository.*;
import RIKU.server.domains.user.repository.UserRepository;
import RIKU.server.domains.point.repository.UserPointRepository;
import RIKU.server.domains.run.participant.repository.ParticipantRepository;
import RIKU.server.domains.run.pacer.repository.PacerRepository;
import RIKU.server.domains.run.comment.repository.CommentRepository;
import RIKU.server.security.AuthMember;
import RIKU.server.global.response.BaseResponseStatus;
import RIKU.server.domains.run.participant.exception.ParticipantException;
import RIKU.server.domains.run.post.exception.PostException;
import RIKU.server.domains.user.exception.UserException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


@Transactional(readOnly = true)
/** Frozen pre-batch implementation: comparison oracle only. */
public class LegacyAttendanceOperations {
    public LegacyAttendanceOperations(ApplicationEventPublisher events, PostRepository postRepository, UserPointRepository userPointRepository, ParticipantRepository participantRepository) {
        this.events=events;
        this.postRepository=postRepository;
        this.userPointRepository=userPointRepository;
        this.participantRepository=participantRepository;
    }


    private final ApplicationEventPublisher events;

    private final PostRepository postRepository;
    private final UserPointRepository userPointRepository;
    private final ParticipantRepository participantRepository;


    @Transactional
    public void manualAttendRun(String runType, Long postId, AuthMember authMember, List<ManualAttendParticipantRequest> requests) {
        // 1. 게시글 조회
        Post post = postRepository.findForUpdate(postId)
                .orElseThrow(() -> new PostException(BaseResponseStatus.POST_NOT_FOUND));

        // 2. PostType 검증
        validatePostType(runType, post.getPostType());

        // 3. PostStatus 검증
        validatePostIsOpen(post);

        // 4. 출석 처리 권한 검증
        validatePostCreator(post, authMember);

        // 5. 출석 처리
        requests.forEach(request -> {
            Participant participant = participantRepository.findByPostIdAndUserId(postId, request.getUserId())
                    .orElseThrow(() -> new ParticipantException(BaseResponseStatus.NOT_PARTICIPATED));

            if (request.getIsAttend()) {
                participant.updateParticipantStatus(ParticipantStatus.ATTENDED);
            } else {
                participant.updateParticipantStatus(ParticipantStatus.PENDING);
            }
        });
    }

    @Transactional
    public void fixAttendRun(String runType, Long postId, AuthMember authMember, List<ManualAttendParticipantRequest> requests) {
        // 1. 게시글 조회
        Post post = postRepository.findForUpdate(postId)
                .orElseThrow(() -> new PostException(BaseResponseStatus.POST_NOT_FOUND));

        // 2. PostType 검증
        PostType postType = validatePostType(runType, post.getPostType());

        // 3. PostStatus 검증
        validatePostIsClose(post);

        // 4. 운영진 권한 검증
        if(!authMember.isAdmin()) {
            throw new UserException(BaseResponseStatus.UNAUTHORIZED_USER);
        }

        // 5. 출석 처리
        List<UserPoint> changedPoints = new ArrayList<>();
        for (ManualAttendParticipantRequest request : requests) {
            Participant participant = participantRepository.findByPostIdAndUserId(postId, request.getUserId())
                    .orElseThrow(() -> new ParticipantException(BaseResponseStatus.NOT_PARTICIPATED));

            boolean attend = Boolean.TRUE.equals(request.getIsAttend());
            boolean isCreator = participant.getUser().getId().equals(post.getPostCreator().getId());

            if (attend) {
                if (participant.getParticipantStatus() != ParticipantStatus.ATTENDED) {
                    participant.updateParticipantStatus(ParticipantStatus.ATTENDED);
                }
                if(!userPointRepository.existsByUserIdAndPostId(request.getUserId(), postId)) {
                    try {
                        switch (postType) {
                            case REGULAR -> changedPoints.add(savePoint(participant.getUser(), 10, "정규런 참여", PointType.ADD_REGULAR_JOIN, post));
                            case TRAINING -> changedPoints.add(savePoint(participant.getUser(), 8, "훈련 참여", PointType.ADD_TRAINING_JOIN, post));
                            case EVENT -> changedPoints.add(savePoint(participant.getUser(), 8, "행사 참여", PointType.ADD_EVENT_JOIN, post));
                            case FLASH -> {
                                if (isCreator) {
                                    changedPoints.add(savePoint(participant.getUser(), 7, "번개런 생성", PointType.ADD_FLASH_CREATE, post));
                                } else {
                                    changedPoints.add(savePoint(participant.getUser(), 5, "번개런 참여", PointType.ADD_FLASH_JOIN, post));
                                }
                            }
                        }
                    } catch (org.springframework.dao.DataIntegrityViolationException ignore) {}
                }
            } else {
                if (participant.getParticipantStatus() != ParticipantStatus.ABSENT) {
                    participant.updateParticipantStatus(ParticipantStatus.ABSENT);
                }
                if (userPointRepository.existsByUserIdAndPostId(request.getUserId(), postId)) {
                    changedPoints.addAll(userPointRepository.deleteByUserIdAndPostId(request.getUserId(), postId));
                }
            }
        }

        if (!changedPoints.isEmpty()) events.publishEvent(RankingChanged.points(changedPoints));
    }

    private PostType validatePostType(String runType, PostType postType) {
        try {
            PostType requestType = PostType.valueOf(runType.toUpperCase());
            if (!postType.equals(requestType)) {
                throw new PostException(BaseResponseStatus.INVALID_POST_TYPE);
            }
            return postType;
        } catch (IllegalArgumentException e) {
            throw new PostException(BaseResponseStatus.INVALID_RUN_TYPE);
        }
    }

    private void validatePostCreator(Post post, AuthMember authMember) {
        if (!post.getPostCreator().getId().equals(authMember.getId())){
            throw new UserException(BaseResponseStatus.UNAUTHORIZED_USER);
        }
    }

    private UserPoint savePoint(User user, int point, String description, PointType pointType, Post post) {
        UserPoint userPoint = UserPoint.createWithPost(user, point, description, pointType, post);
        return userPointRepository.save(userPoint);
    }

    private void validatePostIsOpen(Post post) {
        if (!post.getPostStatus().equals(PostStatus.NOW)) {
            throw new PostException(BaseResponseStatus.INVALID_POST_STATUS);
        }
    }

    private void validatePostIsClose(Post post) {
        if (!post.getPostStatus().equals(PostStatus.CLOSED)) {
            throw new PostException(BaseResponseStatus.INVALID_POST_STATUS);
        }
    }
}
