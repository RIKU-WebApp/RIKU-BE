package RIKU.server.Service;

import RIKU.server.Dto.Participant.Response.ParticipantResponseDto;
import RIKU.server.Entity.Board.Post;
import RIKU.server.Entity.Board.PostStatus;
import RIKU.server.Entity.Participant.Participant;
import RIKU.server.Entity.Participant.ParticipantStatus;
import RIKU.server.Entity.User.User;
import RIKU.server.Repository.ParticipantRepository;
import RIKU.server.Repository.PostRepository;
import RIKU.server.Repository.UserRepository;
import RIKU.server.Util.BaseResponseStatus;
import RIKU.server.Util.Exception.Domain.ParticipantException;
import RIKU.server.Util.Exception.Domain.PostException;
import RIKU.server.Util.Exception.Domain.UserException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ParticipantService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final ParticipantRepository participantRepository;


    // 출석 코드 생성
    @Transactional
    public String createAttendanceCode(Long postId, Long userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostException(BaseResponseStatus.POST_NOT_FOUND));

        // 생성자가 맞는 지 확인
        if (!post.getCreatedBy().getId().equals(userId)) {
            throw new UserException(BaseResponseStatus.UNAUTHORIZED_USER);
        }

        // 이미 출석 코드가 존재하는 지 확인
        if (post.getAttendanceCode() != null) {
            return post.getAttendanceCode();
        }

        // 출석 코드 생성 및 저장
        String code = post.createdAttendanceCode();
        postRepository.save(post);

        return code;
    }

    // 러닝 참여하기
    @Transactional
    public ParticipantResponseDto joinRun(Long postId, Long userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostException(BaseResponseStatus.POST_NOT_FOUND));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(BaseResponseStatus.USER_NOT_FOUND));

        Optional<Participant> existParticipant = participantRepository.findByPostAndUser(post, user);
        if (existParticipant.isPresent()) {
            throw new ParticipantException(BaseResponseStatus.ALREADY_PARTICIPATED);
        }
        Participant participant = new Participant(post, user);
        participantRepository.save(participant); // 참여자 목록에 추가

        return ParticipantResponseDto.of(participant);
    }

    // 러닝 출석하기
    @Transactional
    public ParticipantResponseDto attendRun(Long postId, Long userId, String inputCode) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostException(BaseResponseStatus.POST_NOT_FOUND));

        if (!post.getAttendanceCode().equals(inputCode)) {
            throw new ParticipantException(BaseResponseStatus.INVALID_ATTENDANCE_CODE);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(BaseResponseStatus.USER_NOT_FOUND));

        Participant participant = participantRepository.findByPostAndUser(post, user)
                .orElseThrow(() -> new ParticipantException(BaseResponseStatus.NOT_PARTICIPATED));

        // 이미 출석한 경우 예외 발생
        if (participant.getStatus() == ParticipantStatus.ATTENDED) {
            throw new ParticipantException(BaseResponseStatus.ALREADY_ATTENDED);
        }

        participant.attend();
        participantRepository.save(participant);

        return ParticipantResponseDto.of(participant);
    }

    // 출석 종료하기
    @Transactional
    public String closeRun(Long postId, Long userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostException(BaseResponseStatus.POST_NOT_FOUND));

        // 생성자가 맞는 지 확인
        if (!post.getCreatedBy().getId().equals(userId)) {
            throw new UserException(BaseResponseStatus.UNAUTHORIZED_USER);
        }

        // PostStatus를 CLOSED로 변경
        post.setPostStatus(PostStatus.CLOSED);


        // 출석하지 않은 참여자 상태를 ABSENT로 변경
        participantRepository.findByPost(post).forEach(participant -> {
            if(participant.getStatus() != ParticipantStatus.ATTENDED) {
                participant.absent();
            }
        });

        // 변경된 Post와 Participant 저장
        postRepository.save(post);

        return post.getPostStatus().name();
    }
}
