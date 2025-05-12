package RIKU.server.Service;

import RIKU.server.Dto.User.Request.AuthorizePacerRequest;
import RIKU.server.Dto.User.Response.ReadPacersResponse;
import RIKU.server.Dto.User.Response.ReadUsersResponse;
import RIKU.server.Dto.User.Request.UpdateUsersRequest;
import RIKU.server.Entity.Base.BaseStatus;
import RIKU.server.Entity.Participant.ParticipantStatus;
import RIKU.server.Entity.User.User;
import RIKU.server.Entity.User.UserRole;
import RIKU.server.Repository.ParticipantRepository;
import RIKU.server.Repository.UserPointRepository;
import RIKU.server.Repository.UserRepository;
import RIKU.server.Security.AuthMember;
import RIKU.server.Util.BaseResponseStatus;
import RIKU.server.Util.Exception.Domain.UserException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final UserPointRepository userPointRepository;
    private final ParticipantRepository participantRepository;
    private final PasswordEncoder passwordEncoder;

    public List<ReadUsersResponse> getUsers(AuthMember authMember) {
        // 운영진 권한 검증
        if(!authMember.isAdmin()) {
            throw new UserException(BaseResponseStatus.UNAUTHORIZED_USER);
        }

        return userRepository.findAll()
                .stream()
                .map(user -> {
                    int totalPoints = userPointRepository.sumPointsByUser(user);
                    int participationCount = participantRepository.countByUserAndParticipantStatus(user, ParticipantStatus.ATTENDED);
                    return ReadUsersResponse.of(user, totalPoints, participationCount);
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public void updateUsers(AuthMember authMember, List<UpdateUsersRequest> requests) {
        // 운영진 권한 검증
        if(!authMember.isAdmin()) {
            throw new UserException(BaseResponseStatus.UNAUTHORIZED_USER);
        }

        requests.forEach(request -> {
            User user = userRepository.findByStudentId(request.getStudentId())
                    .orElseThrow(() -> new UserException(BaseResponseStatus.USER_NOT_FOUND));

            // 회원 등급 수정
            if (request.getUserRole() != null) {
                updateUserStatus(user, request.getUserRole());
            }

            // 페이서 여부 수정
            if (request.getIsPacer() != null) {
                user.updatePacer(request.getIsPacer());
            }
        });
    }

    // 페이서 조회
    public List<ReadPacersResponse> getPacers(AuthMember authMember) {
        User user = userRepository.findById(authMember.getId())
                .orElseThrow(() -> new UserException(BaseResponseStatus.USER_NOT_FOUND));


        boolean isAuthorized = user.getUserRole() == UserRole.ADMIN ||
                (user.getUserRole() == UserRole.MEMBER && Boolean.TRUE.equals(user.getIsPacer()));
        if (!isAuthorized) {
            throw new UserException(BaseResponseStatus.UNAUTHORIZED_USER);
        }

        return userRepository.findByIsPacer(Boolean.TRUE)
                .stream()
                .map(ReadPacersResponse::of)
                .collect(Collectors.toList());
    }

    // 페이서 업데이트
    @Transactional
    public void authorizePacer(AuthMember authMember, List<AuthorizePacerRequest> requests) {
        // 운영진 권한 검증
        if(!authMember.isAdmin()) {
            throw new UserException(BaseResponseStatus.UNAUTHORIZED_USER);
        }

        requests.forEach(request -> {
            User user = userRepository.findByStudentId(request.getStudentId())
                    .orElseThrow(() -> new UserException(BaseResponseStatus.USER_NOT_FOUND));

            user.updatePacer(request.getIsPacer());
        });
    }

    // 임시 비밀번호 발급
    @Transactional
    public String resetUserPassword(AuthMember authMember, Long userId) {
        // 1. 운영진 권한 검증
        if(!authMember.isAdmin()) {
            throw new UserException(BaseResponseStatus.UNAUTHORIZED_USER);
        }

        // 2. 유저 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(BaseResponseStatus.USER_NOT_FOUND));

        // 3. 임시 비밀번호 생성 (8~20자, 영문+숫자+특수문자 포함)
        String tempPassword = generateTempPassword();

        // 4. 비밀번호 인코딩 후 업데이트
        String encodedPassword = passwordEncoder.encode(tempPassword);
        user.updatePassword(encodedPassword);

        return tempPassword;
    }

    private void updateUserStatus(User user, String userRole) {
        switch (userRole.toUpperCase()) {
            case "NEW_MEMBER" -> {
                user.updateUserRole(UserRole.NEW_MEMBER);
                user.updateStatus(BaseStatus.ACTIVE);
            }
            case "MEMBER" -> {
                user.updateUserRole(UserRole.MEMBER);
                user.updateStatus(BaseStatus.ACTIVE);
            }
            case "ADMIN" -> {
                user.updateUserRole(UserRole.ADMIN);
                user.updateStatus(BaseStatus.ACTIVE);
            }
            case "INACTIVE" -> {
                user.updateStatus(BaseStatus.INACTIVE);
            }
            default -> throw new IllegalArgumentException("Invalid userRole: " + userRole);
        }
    }

    private String generateTempPassword() {
        String upper = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String lower = "abcdefghijklmnopqrstuvwxyz";
        String digits = "0123456789";
        String special = "!@#$%^&*()_+-=[]{};':\"\\|,.<>/?";
        String all = upper + lower + digits + special;

        SecureRandom random = new SecureRandom();
        StringBuilder password = new StringBuilder();

        // 각 조건 최소 1개씩 보장
        password.append(upper.charAt(random.nextInt(upper.length())));
        password.append(lower.charAt(random.nextInt(lower.length())));
        password.append(digits.charAt(random.nextInt(digits.length())));
        password.append(special.charAt(random.nextInt(special.length())));

        // 나머지 채우기
        for (int i = 4; i < 10; i++) {
            password.append(all.charAt(random.nextInt(all.length())));
        }

        // 셔플
        List<Character> passwordChars = password.chars()
                .mapToObj(c -> (char) c)
                .collect(Collectors.toList());
        Collections.shuffle(passwordChars);

        return passwordChars.stream()
                .map(String::valueOf)
                .collect(Collectors.joining());
    }
}
