package RIKU.server.Service;

import RIKU.server.Config.SeasonProvider;
import RIKU.server.Dto.User.Request.UpdateProfileRequest;
import RIKU.server.Dto.User.Request.SignUpUserRequest;
import RIKU.server.Dto.User.Response.ReadUserProfileDetailResponse;
import RIKU.server.Dto.User.Response.ReadUserProfileResponse;
import RIKU.server.Entity.Participant.ParticipantStatus;
import RIKU.server.Entity.User.PointType;
import RIKU.server.Entity.User.User;
import RIKU.server.Entity.User.UserPoint;
import RIKU.server.Repository.ParticipantRepository;
import RIKU.server.Repository.UserPointRepository;
import RIKU.server.Repository.UserRepository;
import RIKU.server.Security.AuthMember;
import RIKU.server.Util.BaseResponseStatus;
import RIKU.server.Util.DateTimeUtils;
import RIKU.server.Util.Exception.Domain.UserException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserPointRepository userPointRepository;
    private final ParticipantRepository participantRepository;
    private final PasswordEncoder passwordEncoder;
    private final S3Uploader s3Uploader;
    private final SeasonProvider seasonProvider;

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
    public ReadUserProfileResponse getProfile(AuthMember authMember, LocalDate date) {
        // 1. 유저 조회
        User user = userRepository.findById(authMember.getId())
                .orElseThrow(() -> new UserException(BaseResponseStatus.USER_NOT_FOUND));

        // 2. 유저 포인트 총합
        LocalDateTime fromUtc = seasonProvider.seasonStartUtc();
        int totalPoints = userPointRepository.sumPointsByUserSince(user, fromUtc);

        // 3. 출석 완료한 참여내역 수
        int participationCount = participantRepository.countByUserAndParticipantStatusAndCreatedAtGreaterThanEqual(user, ParticipantStatus.ATTENDED, fromUtc);

        // 4. 해당 월의 출석 현황 리스트
        LocalDate startDate = date.withDayOfMonth(1);
        LocalDate endDate = startDate.plusMonths(1).minusDays(1);

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);

        List<LocalDateTime> dateTimes = userPointRepository.findAttendanceDatesInMonth(user, PointType.ADD_ATTENDANCE, startDateTime, endDateTime);
        List<LocalDate> attendanceDates = dateTimes.stream()
                .map(DateTimeUtils::toUserLocalDate)
                .distinct()
                .toList();

        return ReadUserProfileResponse.of(user, totalPoints, participationCount, attendanceDates);
    }

    // 마이페이지 상세조회
    public ReadUserProfileDetailResponse getProfileDetail(AuthMember authMember) {
        // 유저 조회
        User user = userRepository.findById(authMember.getId())
                .orElseThrow(() -> new UserException(BaseResponseStatus.USER_NOT_FOUND));

        return ReadUserProfileDetailResponse.of(user);
    }

    // 마이페이지 수정
    @Transactional
    public void updateProfile(AuthMember authMember, UpdateProfileRequest request) {
        // 유저 조회
        User user = userRepository.findById(authMember.getId())
                .orElseThrow(() -> new UserException(BaseResponseStatus.USER_NOT_FOUND));

        // 전화번호 업데이트 (입력값이 있는 경우만 변경)
        String phone = (request.getPhone() != null && !request.getPhone().isEmpty()) ? request.getPhone() : user.getPhone();

        // 비밀번호 업데이트 (입력값이 있는 경우만 변경)
        String password = (request.getPassword() != null && !request.getPassword().isEmpty())
                ? passwordEncoder.encode(request.getPassword())
                : user.getPassword();

        // 프로필 이미지 업데이트
        String profileImageUrl = user.getProfileImageUrl();
        MultipartFile requestImg =  request.getUserProfileImg();

        if (requestImg != null && !requestImg.isEmpty()) {
            try {
                // 기존 이미지가 있다면 삭제
                if (profileImageUrl != null) {
                    s3Uploader.deleteFileByUrl(profileImageUrl);
                }
                // 새 이미지 업로드
                profileImageUrl = s3Uploader.upload(requestImg, "profileImg");
            } catch (IOException e) {
                throw new UserException(BaseResponseStatus.PROFILE_IMAGE_UPLOAD_FAILED);
            }
        } else if (requestImg != null && requestImg.isEmpty()) {
            // 사용자가 기존 프로필 이미지를 삭제하려는 경우
            if (profileImageUrl != null) {
                s3Uploader.deleteFileByUrl(profileImageUrl);
            }
            profileImageUrl = null;
        }

        // 프로필 업데이트
        user.updateProfile(phone, password, profileImageUrl);
    }

    // 출석하기
    @Transactional
    public void attendProfile(AuthMember authMember) {
        // 1. 유저 조회
        User user = userRepository.findById(authMember.getId())
                .orElseThrow(() -> new UserException(BaseResponseStatus.USER_NOT_FOUND));

        // 2. UTC 기준 날짜로 변환
        LocalDate kstToday = LocalDate.now(DateTimeUtils.getDefaultZone());
        LocalDateTime utcStartOfToday = DateTimeUtils.startOfDayUtc(kstToday);
        LocalDateTime utcEndOfToday = DateTimeUtils.endOfDayUtc(kstToday);

        // 3. 출석 중복 확인 (UTC 기준 검사)
        if(userPointRepository.existsByUserAndPointTypeAndCreatedAtBetween(user, PointType.ADD_ATTENDANCE, utcStartOfToday, utcEndOfToday)) {
            throw new UserException(BaseResponseStatus.ALREADY_ATTENDED_TODAY);
        }

        // 3. 출석 포인트 1점 적립
        UserPoint userPoint = UserPoint.create(user, 1, "오늘의 출석", PointType.ADD_ATTENDANCE);
        userPointRepository.save(userPoint);
    }
}
