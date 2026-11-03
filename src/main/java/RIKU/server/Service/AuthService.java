package RIKU.server.Service;

import RIKU.server.Dto.User.Request.LoginUserRequest;
import RIKU.server.Dto.User.Response.LoginUserResponse;
import RIKU.server.Dto.User.Response.UpdateUserRoleResponse;
import RIKU.server.Entity.User.User;
import RIKU.server.Entity.User.UserRole;
import RIKU.server.Repository.UserRepository;
import RIKU.server.Security.AuthMember;
import RIKU.server.Security.JwtInfo;
import RIKU.server.Security.JwtTokenProvider;
import RIKU.server.Util.BaseResponseStatus;
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
        //Spring Security 사용자 인증
        Authentication authenticationToken = new UsernamePasswordAuthenticationToken(request.getStudentId(), request.getPassword());
        Authentication authentication = authenticationManagerBuilder.getObject().authenticate(authenticationToken);

        // 새로운 JWT 토큰 발급
        JwtInfo jwtInfo = jwtTokenProvider.createToken(authentication);

        // 사용자 정보 반환
        AuthMember principal = (AuthMember) authentication.getPrincipal();
        return LoginUserResponse.of(principal.getId(), principal.getUsername(), jwtInfo);

    }

    public boolean checkStudentIdDuplicate(String studentId) {
        return userRepository.existsByStudentId(studentId);
    }
}
