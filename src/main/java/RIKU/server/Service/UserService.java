package RIKU.server.Service;

import RIKU.server.Dto.User.Request.UpdateProfileRequest;
import RIKU.server.Dto.User.Request.SignUpUserRequest;
import RIKU.server.Dto.User.Response.ReadUserProfileResponse;
import RIKU.server.Entity.User.PointType;
import RIKU.server.Entity.User.User;
import RIKU.server.Entity.User.UserPoint;
import RIKU.server.Repository.UserPointRepository;
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
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserPointRepository userPointRepository;
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

    @Transactional
    public void attendProfile(AuthMember authMember) {
        // 1. 유저 조회
        User user = userRepository.findById(authMember.getId())
                .orElseThrow(() -> new UserException(BaseResponseStatus.USER_NOT_FOUND));

        // 2. 오늘 이미 출석한 경우 예외처리
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        LocalDateTime endOfToday = startOfToday.plusDays(1).minusNanos(1);

        if(userPointRepository.existsByUserAndPointTypeAndCreatedAtBetween(user, PointType.ADD_ATTENDANCE, startOfToday, endOfToday)) {
            throw new UserException(BaseResponseStatus.ALREADY_ATTENDED_TODAY);
        }

        // 3. 출석 포인트 1점 적립
        UserPoint userPoint = UserPoint.create(user, 1, "마이페이지 출석", PointType.ADD_ATTENDANCE);
        userPointRepository.save(userPoint);
    }
}
