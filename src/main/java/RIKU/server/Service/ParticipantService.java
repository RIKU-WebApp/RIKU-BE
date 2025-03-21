package RIKU.server.Service;

import RIKU.server.Dto.Participant.Response.UpdateParticipantResponse;
import RIKU.server.Entity.Board.Post.*;
import RIKU.server.Entity.Board.PostStatus;
import RIKU.server.Entity.Participant.Participant;
import RIKU.server.Entity.Participant.ParticipantStatus;
import RIKU.server.Entity.User.User;
import RIKU.server.Repository.*;
import RIKU.server.Security.AuthMember;
import RIKU.server.Util.BaseResponseStatus;
import RIKU.server.Util.Exception.Domain.ParticipantException;
import RIKU.server.Util.Exception.Domain.PostException;
import RIKU.server.Util.Exception.Domain.UserException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final ParticipantRepository participantRepository;


    // 출석 코드 생성
    @Transactional
    public String createAttendanceCode(String runType, Long postId, AuthMember authMember) {
        // 1. 게시글 조회
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostException(BaseResponseStatus.POST_NOT_FOUND));

        // 2. PostType 검증
        PostType postType = validatePostType(runType, post.getPostType());

        // 3. 출석 코드 생성자 검증
        validatePermission(post, postType, authMember);

        // 4. 기존 출석 코드가 존재하면 반환
        return getExistingAttendanceCode(post, postType)
                .orElseGet(() -> {
                    // 5. 출석 코드 생성 및 저장
                    String code = generateAttendanceCode();
                    saveAttendanceCode(post, postType, code);
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
        PostType postType = validatePostType(runType, post.getPostType());

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
        validatePermission(post, postType, authMember);

        // 4. 출석 종료 처리
        post.updatePostStatus(PostStatus.CLOSED);

        // 5. 출석하지 않은 참여자 상태를 ABSENT로 변경
        participantRepository.findByPost(post).forEach(participant -> {
            if(participant.getParticipantStatus() != ParticipantStatus.ATTENDED) {
                participant.absent();
            }
        });
    }

    private PostType validatePostType(String runType, PostType postType) {
        try {
            PostType requestType = PostType.valueOf(runType.toUpperCase());
            if (!postType.equals(requestType)) {
                throw new PostException(BaseResponseStatus.INVALID_POST_TYPE);
            }
            if (postType == PostType.EVENT) {
                throw new PostException(BaseResponseStatus.UNAUTHORIZED_POST_TYPE);
            }
            return postType;
        } catch (IllegalArgumentException e) {
            throw new PostException(BaseResponseStatus.INVALID_RUN_TYPE);
        }
    }
    private void validatePermission(Post post, PostType postType, AuthMember authMember) {
        if (postType == PostType.FLASH) {
            // 번개런이면 생성자 권한
            if (!post.getPostCreator().getId().equals(authMember.getId())){
                throw new UserException(BaseResponseStatus.UNAUTHORIZED_USER);

            }
        } else {
            // 번개런이 아닌 경우 운영진 권한
            if (!authMember.isAdmin()) {
                throw new UserException(BaseResponseStatus.UNAUTHORIZED_USER);
            }
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
}