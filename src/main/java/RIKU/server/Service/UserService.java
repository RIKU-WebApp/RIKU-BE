package RIKU.server.Service;

import RIKU.server.Dto.User.Request.UserLoginRequestDto;
import RIKU.server.Dto.User.Request.UserSignUpRequestDto;
import RIKU.server.Dto.User.Response.UserLoginResponseDto;
import RIKU.server.Entity.User.User;
import RIKU.server.Repository.UserRepository;
import RIKU.server.Util.BaseResponseStatus;
import RIKU.server.Util.Exception.Domain.UserException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 회원 가입
     */
    @Transactional
    public Long signUp(UserSignUpRequestDto requestDto) {
        // TODO 1. 로그인 아이디 중복 체크
        if(userRepository.existsByLoginId(requestDto.getLoginId())) throw new UserException(BaseResponseStatus.DUPLICATED_LOGINID);

        // TODO 2. DTO를 Entity로 변환
        String encodedPassword = passwordEncoder.encode(requestDto.getPassword());
        User user = requestDto.toEntity(encodedPassword);

        return userRepository.save(user).getId();
    }

    @Transactional
    public UserLoginResponseDto login(UserLoginRequestDto requestDto) {
        // TODO 1. 로그인 아이디 조회
        User user = userRepository.findByLoginId(requestDto.getLoginId())
                .orElseThrow(() -> new UserException(BaseResponseStatus.USER_NOT_FOUND));

        // TODO 2. 비밀번호 검증
        if(!passwordEncoder.matches(requestDto.getPassword(), user.getPassword())) {
            throw new UserException(BaseResponseStatus.INVALID_PASSWORD);
        }

        return UserLoginResponseDto.of(user);
    }

}
