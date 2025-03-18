package RIKU.server.Service;

import RIKU.server.Dto.User.Request.UpdateProfileRequest;
import RIKU.server.Dto.User.Request.SignUpUserRequest;
import RIKU.server.Dto.User.Response.ReadPacersResponse;
import RIKU.server.Dto.User.Response.ReadUserProfileResponse;
import RIKU.server.Entity.User.User;
import RIKU.server.Repository.UserRepository;
import RIKU.server.Security.AuthMember;
import RIKU.server.Util.BaseResponseStatus;
import RIKU.server.Util.Exception.Domain.UserException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

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
    public Long signUp(SignUpUserRequest request) {
        // 1. 로그인 아이디 중복 체크
        if(userRepository.existsByStudentId(request.getStudentId())) throw new UserException(BaseResponseStatus.DUPLICATED_STUDENTID);

        // 2. DTO -> Entity로 변환
        String encodedPassword = passwordEncoder.encode(request.getPassword());
        User user = request.toEntity(request, encodedPassword);

        return userRepository.save(user).getId();
    }

    // 마이페이지 조회
    public ReadUserProfileResponse getProfile(Long userId) {
        // 유저 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(BaseResponseStatus.USER_NOT_FOUND));

        return ReadUserProfileResponse.of(user);
    }

    // 마이페이지 수정
    @Transactional
    public Long updateProfile(Long userId, UpdateProfileRequest request) {
        // 유저 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(BaseResponseStatus.USER_NOT_FOUND));

        // 전화번호 업데이트 (입력값이 있는 경우만 변경)
        String phone = (request.getPhone() != null && !request.getPhone().isEmpty()) ? request.getPhone() : user.getPhone();

        // 비밀번호 업데이트
        String password = (request.getPassword() != null && !request.getPassword().isEmpty())
                ? passwordEncoder.encode(request.getPassword())
                : user.getPassword();

        // 프로필 이미지 업데이트
        String profileImageUrl = user.getProfileImageUrl();
        if (request.getUserProfileImg() != null && !request.getUserProfileImg().isEmpty()) {
            try {
                log.debug("Received profile image: {}", request.getUserProfileImg().getOriginalFilename());
                profileImageUrl = s3Uploader.upload(request.getUserProfileImg(), "profileImg");
                log.debug("Profile image uploaded: {}", profileImageUrl);
            } catch (IOException e) {
                log.error("File upload failed: {}", request.getUserProfileImg().getOriginalFilename(), e);
                throw new UserException(BaseResponseStatus.PROFILE_IMAGE_UPLOAD_FAILED);
            }
        } else if (request.getUserProfileImg() == null) {
            // 사용자가 기존 프로필 이미지를 삭제하려는 경우
            profileImageUrl = null;
            // TODO: S3 이미지 삭제 로직 추가
        }

        // 프로필 업데이트
        user.updateProfile(phone, password, profileImageUrl);

        return user.getId();
    }

    // 페이서 조회
    public List<ReadPacersResponse> getPacers(AuthMember authMember) {
        // 운영진 권한 검증
        if(!authMember.isAdmin()) {
            throw new UserException(BaseResponseStatus.UNAUTHORIZED_USER);
        }

        return userRepository.findByIsPacer(Boolean.TRUE)
                .stream()
                .map(ReadPacersResponse::of)
                .collect(Collectors.toList());
    }
}
