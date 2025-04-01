package RIKU.server.Service;

import RIKU.server.Dto.User.Request.AuthorizePacerRequest;
import RIKU.server.Dto.User.Response.ReadPacersResponse;
import RIKU.server.Dto.User.Response.ReadUsersResponse;
import RIKU.server.Dto.User.Request.UpdateUserRoleRequest;
import RIKU.server.Entity.Base.BaseStatus;
import RIKU.server.Entity.Participant.ParticipantStatus;
import RIKU.server.Entity.User.User;
import RIKU.server.Repository.ParticipantRepository;
import RIKU.server.Repository.UserPointRepository;
import RIKU.server.Repository.UserRepository;
import RIKU.server.Security.AuthMember;
import RIKU.server.Util.BaseResponseStatus;
import RIKU.server.Util.Exception.Domain.UserException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final UserPointRepository userPointRepository;
    private final ParticipantRepository participantRepository;

    public List<ReadUsersResponse> getUsers(AuthMember authMember) {
        // 운영진 권한 검증
        if(!authMember.isAdmin()) {
            throw new UserException(BaseResponseStatus.UNAUTHORIZED_USER);
        }

        return userRepository.findByStatus(BaseStatus.ACTIVE)
                .stream()
                .map(user -> {
                    int totalPoints = userPointRepository.sumPointsByUser(user);
                    int participationCount = participantRepository.countByUserAndParticipantStatus(user, ParticipantStatus.ATTENDED);
                    return ReadUsersResponse.of(user, totalPoints, participationCount);
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public void updateUsers(AuthMember authMember, List<UpdateUserRoleRequest> requests) {
        // 운영진 권한 검증
        if(!authMember.isAdmin()) {
            throw new UserException(BaseResponseStatus.UNAUTHORIZED_USER);
        }

        requests.forEach(request -> {
            User user = userRepository.findByStudentId(request.getStudentId())
                    .orElseThrow(() -> new UserException(BaseResponseStatus.USER_NOT_FOUND));

            user.updateUserRole(request.getUserRole());
        });
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
}
