package RIKU.server.Service;

import RIKU.server.Dto.User.UserSignUpRequestDto;
import RIKU.server.Entity.User.User;
import RIKU.server.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    /**
     * 회원 가입
     */
    @Transactional
    public Long signUp(UserSignUpRequestDto requestDto) {
        // TODO 1: 로그인 아이디 중복 체크
        userRepository.findByLoginId(requestDto.getLoginId())
                .ifPresent(user -> {
                    throw new RuntimeException(requestDto.getLoginId() + "는 이미 존재합니다.");
                });

        // TODO 2. DTO를 Entity로 변환
        User user = requestDto.toEntity();

        return userRepository.save(user).getId();
    }

}
