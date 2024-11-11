package RIKU.server.Security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@AllArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtTokenProvider jwtTokenProvider;
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String token = resolveToken(request);

        log.debug("Request URI: " + request.getRequestURI());
        log.debug("Token resolved: " + token);

        /**
         * @Breif 토큰 검증 후 Spring Security Context에 인증 정보 담음
         * @Condition 토큰 값이 존재하고, 검증 되었으면 실행
         */
        if (token != null && jwtTokenProvider.validateToken(token)) {
            Authentication authentication = jwtTokenProvider.getAuthentication(token);
            SecurityContextHolder.getContext().setAuthentication(authentication);log.debug("Authentication set in SecurityContextHolder: " + authentication);
        } else {
            log.debug("No valid token found or token validation failed.");
        }
        filterChain.doFilter(request,response);
    }

    /**
     * @Brief 토큰 파싱하여 Bearer 타입인지 확인하고 그 부분 잘라내서 반환
     * @return 토큰 값(Bearer 제외)
     */
    private String resolveToken(HttpServletRequest request) {
        String authorization = request.getHeader(AUTHORIZATION);
        if (StringUtils.hasText(authorization) && authorization.startsWith(jwtTokenProvider.BEARER)) {
            return authorization.substring(jwtTokenProvider.BEARER.length());
        }

        return null;
    }

    /**
     * @Brief 토큰 검증하고 인가 처리
     * @param token
     */
    private void tokenValidateAndAuthorization(String token) {

        if (jwtTokenProvider.validateToken(token)) {
            setAuthentication(jwtTokenProvider.getAuthentication(token));
        }

    }

    private void setAuthentication(Authentication authentication) {
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

}
