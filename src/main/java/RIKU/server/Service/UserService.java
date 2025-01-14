package RIKU.server.Service;

import RIKU.server.Dto.User.Request.UpdateProfileRequestDto;
import RIKU.server.Dto.User.Request.UserSignUpRequestDto;
import RIKU.server.Dto.User.Response.ReadUserProfileResponseDto;
import RIKU.server.Entity.User.User;
import RIKU.server.Repository.UserRepository;
import RIKU.server.Util.BaseResponseStatus;
import RIKU.server.Util.Exception.Domain.UserException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;

@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final S3Uploader s3Uploader;

    // 회원가입
    @Transactional
    public Long signUp(UserSignUpRequestDto requestDto) {
        // TODO 1. 로그인 아이디 중복 체크
        if(userRepository.existsByStudentId(requestDto.getStudentId())) throw new UserException(BaseResponseStatus.DUPLICATED_STUDENTID);

        // TODO 2. DTO를 Entity로 변환
        String encodedPassword = passwordEncoder.encode(requestDto.getPassword());
        User user = requestDto.toEntity(encodedPassword);

        return userRepository.save(user).getId();
    }

    // 마이페이지 조회
    public ReadUserProfileResponseDto getProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(BaseResponseStatus.USER_NOT_FOUND));

        return ReadUserProfileResponseDto.of(user);
    }

    // 마이페이지 수정
    @Transactional
    public Long updateProfile(Long userId, UpdateProfileRequestDto requestDto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(BaseResponseStatus.USER_NOT_FOUND));

        // 전화번호 업데이트
        if(requestDto.getPhone() != null && !requestDto.getPhone().isEmpty()) {
            user.setPhone(requestDto.getPhone());
        }

        // 비밀번호 업데이트
        if(requestDto.getPassword() != null && !requestDto.getPassword().isEmpty()) {
            String encodedPassword = passwordEncoder.encode(requestDto.getPassword());
            user.setPassword(encodedPassword);
        }

        // 프로필 이미지 업데이트
        String profileImageUrl = user.getProfileImageUrl();
        if (requestDto.getUserProfileImg() != null && !requestDto.getUserProfileImg().isEmpty()) {
            try {
                profileImageUrl = s3Uploader.upload(requestDto.getUserProfileImg(), "profileImg");
            } catch (IOException e) {
                log.error("File upload failed: {}", requestDto.getUserProfileImg().getOriginalFilename(), e);
                throw new UserException(BaseResponseStatus.PROFILE_IMAGE_UPLOAD_FAILED);
            }
        } else {
            // 이미지를 없애는 경우
            profileImageUrl = null;
        }
        user.setProfileImageUrl(profileImageUrl);

        return userRepository.save(user).getId();
    }
}
