package RIKU.server.Service;

import RIKU.server.Dto.User.Response.ReadUsersResponseDto;
import RIKU.server.Dto.User.UserRoleDto;
import RIKU.server.Entity.User.User;
import RIKU.server.Entity.User.UserRole;
import RIKU.server.Repository.UserRepository;
import RIKU.server.Security.AuthMember;
import RIKU.server.Util.BaseResponseStatus;
import RIKU.server.Util.Exception.Domain.UserException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;

    public List<ReadUsersResponseDto> getUsers(AuthMember authMember) {
        // 운영진 권한 검증
        if(!authMember.isAdmin()) {
            throw new UserException(BaseResponseStatus.UNAUTHORIZED_USER);
        }

        return userRepository.findAll()
                .stream()
                .map(ReadUsersResponseDto::of)
                .collect(Collectors.toList());
    }

    @Transactional
    public List<UserRoleDto> updateUsers(AuthMember authMember, List<UserRoleDto> requests) {
        // 운영진 권한 검증
        if(!authMember.isAdmin()) {
            throw new UserException(BaseResponseStatus.UNAUTHORIZED_USER);
        }

        return requests.stream()
                .map(request -> {
                    User user = userRepository.findByStudentId(request.getStudentId())
                            .orElseThrow(() -> new UserException(BaseResponseStatus.USER_NOT_FOUND));
                    user.setUserRole(request.getUserRole());
                    userRepository.save(user);
                    return UserRoleDto.of(user);
                }).collect(Collectors.toList());
    }
}
