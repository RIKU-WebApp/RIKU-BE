package RIKU.server.Service;

import RIKU.server.Dto.Participant.Request.ManualAttendParticipantRequest;
import RIKU.server.Dto.Participant.Response.UpdateParticipantResponse;
import RIKU.server.Entity.Board.Post.*;
import RIKU.server.Entity.Board.PostStatus;
import RIKU.server.Entity.Participant.Participant;
import RIKU.server.Entity.Participant.ParticipantStatus;
import RIKU.server.Entity.User.PointType;
import RIKU.server.Entity.User.User;
import RIKU.server.Entity.User.UserPoint;
import RIKU.server.Repository.*;
import RIKU.server.Security.AuthMember;
import RIKU.server.Util.BaseResponseStatus;
import RIKU.server.Util.Exception.Domain.ParticipantException;
import RIKU.server.Util.Exception.Domain.PostException;
import RIKU.server.Util.Exception.Domain.UserException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ParticipantService {

    private final PostRepository postRepository;
    private final FlashPostRepository flashPostRepository;
    private final RegularPostRepository regularPostRepository;
    private final TrainingPostRepository trainingPostRepository;
    private final UserRepository userRepository;
    private final UserPointRepository userPointRepository;
    private final ParticipantRepository participantRepository;


    // 출석 코드 생성
    @Transactional
    public String createAttendanceCode(String runType, Long postId, AuthMember authMember) {
        // 1. 게시글 조회
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostException(BaseResponseStatus.POST_NOT_FOUND));

        // 2. PostType 검증
        PostType postType = validatePostType(runType, post.getPostType());

        if (postType == PostType.EVENT) {
            throw new PostException(BaseResponseStatus.UNAUTHORIZED_POST_TYPE);
        }

        // 3. 출석 코드 생성자 검증
        validatePostCreator(post, authMember);

        // 4. 날짜 검증
        LocalDateTime now = LocalDateTime.now();
        if (post.getDate().isAfter(now)) {
            throw new ParticipantException(BaseResponseStatus.INVALID_ATTENDANCE_TIME);
        }

        // 5. 기존 출석 코드가 존재하면 반환
        return getExistingAttendanceCode(post, postType)
                .orElseGet(() -> {
                    // 6. 출석 코드 생성 및 저장
                    String code = generateAttendanceCode();
                    saveAttendanceCode(post, postType, code);

                    // 7. 생성자 출석 처리
                    Participant participant = participantRepository.findByPostIdAndUserId(postId, authMember.getId())
                            .orElseThrow(() -> new ParticipantException(BaseResponseStatus.NOT_PARTICIPATED));
                    if (participant.getParticipantStatus() != ParticipantStatus.ATTENDED) {
                        participant.attend();
                    }

                    return code;
                });
    }

    // 러닝 참여하기
    @Transactional
    public UpdateParticipantResponse joinRun(String runType, Long postId, AuthMember authMember) {
        // 1. 게시글 조회
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostException(BaseResponseStatus.POST_NOT_FOUND));

        // 2. 유저 조회
        User user = userRepository.findById(authMember.getId())
                .orElseThrow(() -> new UserException(BaseResponseStatus.USER_NOT_FOUND));

        // 3. PostType 검증
        validatePostType(runType, post.getPostType());

        // 4. 이미 참여한 경우 예외 처리
        if (participantRepository.existsByPostAndUser(post, user)) {
            throw new ParticipantException(BaseResponseStatus.ALREADY_PARTICIPATED);
        }

        // 5. 새로운 참여자 생성 후 저장
        Participant participant = Participant.create(post, user);
        participantRepository.save(participant);

        return UpdateParticipantResponse.of(participant);
    }

    // 러닝 출석하기
    @Transactional
    public UpdateParticipantResponse attendRun(String runType, Long postId, AuthMember authMember, String inputCode) {
        // 1. 게시글 조회
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostException(BaseResponseStatus.POST_NOT_FOUND));

        // 2. 유저 조회
        User user = userRepository.findById(authMember.getId())
                .orElseThrow(() -> new UserException(BaseResponseStatus.USER_NOT_FOUND));

        // 3. PostType 검증
        PostType postType = validatePostType(runType, post.getPostType());

        if (postType == PostType.EVENT) {
            throw new PostException(BaseResponseStatus.UNAUTHORIZED_POST_TYPE);
        }

        // 4. 출석 코드 조회 및 검증
        Optional<String> storedCode = getExistingAttendanceCode(post, postType);
        if (storedCode.isPresent() && !storedCode.get().equals(inputCode)) {
            throw new ParticipantException(BaseResponseStatus.INVALID_ATTENDANCE_CODE);
        }

        // 5. 참여자 조회
        Participant participant = participantRepository.findByPostAndUser(post, user)
                .orElseThrow(() -> new ParticipantException(BaseResponseStatus.NOT_PARTICIPATED));

        // 6. 이미 출석한 경우 예외 발생
        if (participant.getParticipantStatus() == ParticipantStatus.ATTENDED) {
            throw new ParticipantException(BaseResponseStatus.ALREADY_ATTENDED);
        }

        // 7. 출석 상태 변경
        participant.attend();

        return UpdateParticipantResponse.of(participant);
    }

    // 출석 종료하기
    @Transactional
    public void closeRun(String runType, Long postId, AuthMember authMember) {
        // 1. 게시글 조회
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostException(BaseResponseStatus.POST_NOT_FOUND));

        // 2. PostType 검증
        PostType postType = validatePostType(runType, post.getPostType());

        // 3. 출석 종료 권한 검증
        validatePostCreator(post, authMember);

        // 4. 이미 종료된 게시글이라면 예외 발생
        if (post.getPostStatus() == PostStatus.CLOSED) {
            throw new PostException(BaseResponseStatus.ALREADY_CLOSED_POST);
        }

        // 5. 출석 종료 처리
        post.updatePostStatus(PostStatus.CLOSED);

        // 6. 결석자 처리 및 출석 포인트 일괄 지급
        List<Participant> participants = participantRepository.findByPost(post);

        for (Participant participant : participants) {
            if (participant.getParticipantStatus() == ParticipantStatus.ATTENDED) {
                // 번개런 생성자는 출석 포인트 지급 제외
                boolean isFlashCreator = post.getPostType() == PostType.FLASH && participant.getUser().getId().equals(post.getPostCreator().getId());

                if (!isFlashCreator) {
                    switch (postType) {
                        case REGULAR -> savePoint(participant.getUser(), 10, "정규런 참여", PointType.ADD_REGULAR_JOIN, post);
                        case FLASH -> savePoint(participant.getUser(), 5, "번개런 참여", PointType.ADD_FLASH_JOIN, post);
                        case TRAINING -> savePoint(participant.getUser(), 8, "훈련 참여", PointType.ADD_TRAINING_JOIN, post);
                        case EVENT -> savePoint(participant.getUser(), 8, "행사 참여", PointType.ADD_EVENT_JOIN, post);
                    }
                }
            } else {
                participant.absent();   // 출석하지 않은 참여자 상태 ABSENT로 변경
            }
        }

        // 7. 번개런 생성 포인트 적립
        if (postType == PostType.FLASH) {
            savePoint(post.getPostCreator(), 7, "번개런 생성", PointType.ADD_FLASH_CREATE, post);
        }
    }

    // 수동 출석 처리
    @Transactional
    public void manualAttendRun(String runType, Long postId, AuthMember authMember, List<ManualAttendParticipantRequest> requests) {
        // 1. 게시글 조회
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostException(BaseResponseStatus.POST_NOT_FOUND));

        // 2. PostType 검증
        validatePostType(runType, post.getPostType());

        // 3. 출석 처리 권한 검증
        validatePostCreator(post, authMember);

        // 4. 출석 처리
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

    private Optional<String> getExistingAttendanceCode(Post post, PostType postType) {
        switch (postType) {
            case FLASH:
                return flashPostRepository.findByPost(post)
                        .map(FlashPost::getAttendanceCode);
            case REGULAR:
                return regularPostRepository.findByPost(post)
                        .map(RegularPost::getAttendanceCode);
            case TRAINING:
                return trainingPostRepository.findByPost(post)
                        .map(TrainingPost::getAttendanceCode);
            default:
                return Optional.empty();
        }
    }

    private String generateAttendanceCode() {
        return String.valueOf((int) (Math.random() * 900) + 100);
    }

    private void saveAttendanceCode(Post post, PostType postType, String code) {
        switch (postType) {
            case FLASH:
                flashPostRepository.findByPost(post)
                        .ifPresentOrElse(flashPost -> {
                            flashPost.updateAttendanceCode(code);
                        }, () -> {throw new PostException(BaseResponseStatus.POST_NOT_FOUND);});
                break;
            case REGULAR:
                regularPostRepository.findByPost(post)
                        .ifPresentOrElse(regularPost -> {
                            regularPost.updateAttendanceCode(code);
                        }, () -> {throw new PostException(BaseResponseStatus.POST_NOT_FOUND);});
                break;
            case TRAINING:
                trainingPostRepository.findByPost(post)
                        .ifPresentOrElse(trainingPost -> {
                            trainingPost.updateAttendanceCode(code);
                        }, () -> {throw new PostException(BaseResponseStatus.POST_NOT_FOUND);});
                break;
        }
    }

    private void savePoint(User user, int point, String description, PointType pointType, Post post) {
        UserPoint userPoint = UserPoint.createWithPost(user, point, description, pointType, post);
        userPointRepository.save(userPoint);
    }
}