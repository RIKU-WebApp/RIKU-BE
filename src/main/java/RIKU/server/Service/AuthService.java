package RIKU.server.Service;

import RIKU.server.Dto.User.Request.LoginUserRequest;
import RIKU.server.Dto.User.Request.RefreshTokenRequest;
import RIKU.server.Dto.User.Response.LoginUserResponse;
import RIKU.server.Dto.User.Response.UpdateUserRoleResponse;
import RIKU.server.Entity.User.User;
import RIKU.server.Entity.User.UserRole;
import RIKU.server.Repository.UserRepository;
import RIKU.server.Security.AuthMember;
import RIKU.server.Security.JwtInfo;
import RIKU.server.Security.JwtTokenProvider;
import RIKU.server.Util.BaseResponseStatus;
import RIKU.server.Util.Exception.CustomJwtException;
import RIKU.server.Util.Exception.Domain.UserException;
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
        // Spring Security 사용자 인증
        Authentication authenticationToken = new UsernamePasswordAuthenticationToken(request.getStudentId(), request.getPassword());
        Authentication authentication = authenticationManagerBuilder.getObject().authenticate(authenticationToken);

        // 새로운 JWT 토큰 발급
        JwtInfo jwtInfo = jwtTokenProvider.createToken(authentication);

        // refreshToken 저장
        AuthMember principal = (AuthMember) authentication.getPrincipal();
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new UserException(BaseResponseStatus.USER_NOT_FOUND));
        String rawRefreshToken = jwtInfo.getRefreshToken().substring(JwtTokenProvider.BEARER.length());
        user.updateRefreshToken(rawRefreshToken);

        return LoginUserResponse.of(principal.getId(), principal.getUsername(), jwtInfo);
    }

    @Transactional
    public LoginUserResponse reissue(RefreshTokenRequest request) {
        // Bearer 제거
        String rawToken = request.getRefreshToken().startsWith(JwtTokenProvider.BEARER)
                ? request.getRefreshToken().substring(JwtTokenProvider.BEARER.length())
                : request.getRefreshToken();

        // refresh token 검증
        Long userId = jwtTokenProvider.validateRefreshTokenAndGetUserId(rawToken);

        // 유저 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(BaseResponseStatus.USER_NOT_FOUND));

        // db의 refreshToken과 일치 확인
        if (!rawToken.equals(user.getRefreshToken())) {
            throw new CustomJwtException(BaseResponseStatus.INVALID_REFRESH_TOKEN);
        }

        // 새 토큰 발급
        AuthMember authMember = AuthMember.builder()
                .id(user.getId())
                .username(user.getStudentId())
                .authorities(AuthorityUtils.createAuthorityList(user.getUserRole().getRole()))
                .status(user.getStatus())
                .build();
        Authentication authentication = new UsernamePasswordAuthenticationToken(authMember, null, authMember.getAuthorities());
        JwtInfo newJwtInfo = jwtTokenProvider.createToken(authentication);

        // 새 refreshToken 저장
        String newRawRefreshToken = newJwtInfo.getRefreshToken().substring(JwtTokenProvider.BEARER.length());
        user.updateRefreshToken(newRawRefreshToken);

        return LoginUserResponse.of(user.getId(), user.getStudentId(), newJwtInfo);
    }

    @Transactional
    public void logout(RefreshTokenRequest request) {
        // Bearer 제거
        String rawToken = request.getRefreshToken().startsWith(JwtTokenProvider.BEARER)
                ? request.getRefreshToken().substring(JwtTokenProvider.BEARER.length())
                : request.getRefreshToken();

        // 유저 조회
        Long userId = jwtTokenProvider.validateRefreshTokenAndGetUserId(rawToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(BaseResponseStatus.USER_NOT_FOUND));

        // refreshToken 삭제
        user.updateRefreshToken(null);
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
