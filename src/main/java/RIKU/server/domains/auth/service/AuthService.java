package RIKU.server.domains.auth.service;

import RIKU.server.domains.auth.dto.request.LoginUserRequest;
import RIKU.server.domains.auth.dto.response.LoginUserResponse;
import RIKU.server.domains.auth.dto.response.UpdateUserRoleResponse;
import RIKU.server.domains.user.entity.User;
import RIKU.server.domains.user.entity.UserRole;
import RIKU.server.domains.user.repository.UserRepository;
import RIKU.server.security.AuthMember;
import RIKU.server.security.JwtInfo;
import RIKU.server.security.JwtTokenProvider;
import RIKU.server.global.response.BaseResponseStatus;
import RIKU.server.domains.user.exception.UserException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AuthService {

    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManagerBuilder authenticationManagerBuilder;
    private final UserRepository userRepository;

    @Transactional
    public LoginUserResponse login(LoginUserRequest request) {
        //Spring Security 사용자 인증
        Authentication authenticationToken = new UsernamePasswordAuthenticationToken(request.getStudentId(), request.getPassword());
        Authentication authentication = authenticationManagerBuilder.getObject().authenticate(authenticationToken);

        // 새로운 JWT 토큰 발급
        JwtInfo jwtInfo = jwtTokenProvider.createToken(authentication);

        // 사용자 정보 반환
        AuthMember principal = (AuthMember) authentication.getPrincipal();
        return LoginUserResponse.of(principal.getId(), principal.getUsername(), jwtInfo);

    }

    @Transactional
    public UpdateUserRoleResponse updateUserRole(Long userId, UserRole newRole) {
        // 유저 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(BaseResponseStatus.USER_NOT_FOUND));

        // 중복 변경 방지
        if (user.getUserRole().equals(newRole)) {
            throw new UserException(BaseResponseStatus.ROLE_ALREADY_ASSIGNED);
        }

        // db 역할 변경
        user.updateUserRole(newRole);

        // AuthMember 객체 생성
        AuthMember authMember = AuthMember.builder()
                .id(user.getId())
                .username(user.getStudentId())
                .authorities(AuthorityUtils.createAuthorityList(newRole.getRole()))
                .build();

        // 변경된 역할로 새로운 JWT 토큰 발급
        Authentication authentication = new UsernamePasswordAuthenticationToken(authMember, null, authMember.getAuthorities());
        JwtInfo newJwtInfo = jwtTokenProvider.createToken(authentication);

        // 변경된 역할과 새로운 토큰 정보를 반환
        return UpdateUserRoleResponse.of(user.getId(), user.getUserRole(), newJwtInfo);
    }

    public boolean checkStudentIdDuplicate(String studentId) {
        return userRepository.existsByStudentId(studentId);
    }
}
