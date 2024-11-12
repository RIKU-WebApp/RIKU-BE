package RIKU.server.Service;

import RIKU.server.Dto.Participant.Response.ParticipantResponseDto;
import RIKU.server.Entity.Board.FlashPost;
import RIKU.server.Entity.Board.Post;
import RIKU.server.Entity.Participant.Participant;
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

        if (!post.getCreatedBy().getId().equals(userId)) {
            throw new UserException(BaseResponseStatus.UNAUTHORIZED_USER);
        }

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


}
