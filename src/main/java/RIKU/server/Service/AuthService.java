package RIKU.server.Service;

import RIKU.server.Dto.User.Request.UserLoginRequestDto;
import RIKU.server.Dto.User.Response.UserLoginResponseDto;
import RIKU.server.Dto.User.Response.UserRoleResponseDto;
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
public class AuthService {

    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManagerBuilder authenticationManagerBuilder;
    private final UserRepository userRepository;


    @Transactional
    public UserLoginResponseDto login(UserLoginRequestDto requestDto) {

        Authentication authenticationToken = new UsernamePasswordAuthenticationToken(requestDto.getStudentId(), requestDto.getPassword());

        Authentication authentication = authenticationManagerBuilder.getObject().authenticate(authenticationToken);

        JwtInfo jwtInfo = jwtTokenProvider.createToken(authentication);
        AuthMember principal = (AuthMember) authentication.getPrincipal();

        return UserLoginResponseDto.of(principal.getId(), principal.getUsername(), jwtInfo);

    }

    @Transactional
    public UserRoleResponseDto updateUserRole(Long userId, UserRole newRole) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(BaseResponseStatus.USER_NOT_FOUND));

        user.setUserRole(newRole);
        userRepository.save(user);

        AuthMember authMember = AuthMember.builder()
                .id(user.getId())
                .username(user.getStudentId())
                .authorities(AuthorityUtils.createAuthorityList(newRole.getRole()))
                .build();

        // 변경된 역할로 새로운 JWT 토큰 발급
        Authentication authentication = new UsernamePasswordAuthenticationToken(authMember, null, authMember.getAuthorities());
        JwtInfo newJwtInfo = jwtTokenProvider.createToken(authentication);

        // 변경된 역할과 새로운 토큰 정보를 반환
        return new UserRoleResponseDto(user.getId(), user.getUserRole(), newJwtInfo);
    }
}
