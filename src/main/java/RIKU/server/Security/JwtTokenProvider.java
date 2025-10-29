package RIKU.server.Security;

import RIKU.server.Entity.Base.BaseStatus;
import RIKU.server.Entity.User.User;
import RIKU.server.Repository.UserRepository;
import RIKU.server.Util.BaseResponseStatus;
import RIKU.server.Util.Exception.CustomJwtException;
import RIKU.server.Util.Exception.Domain.UserException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.List;

@Slf4j
@Component
public class JwtTokenProvider {

    @Value("${secret.jwt-access-expired-in}")
    private Long JWT_ACCESS_EXPIRED_IN;

    @Value("${secret.jwt-refresh-expired-in}")
    private Long JWT_REFRESH_EXPIRED_IN;

    private final Key key;

    private final UserRepository userRepository;

    public static final String BEARER = "Bearer ";

    public JwtTokenProvider(@Value("${secret.jwt-secret-key}") String JWT_SECRET_KEY, UserRepository userRepository) {
        this.userRepository = userRepository;
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(JWT_SECRET_KEY));
    }

    public JwtInfo createToken(Authentication authentication) {
        AuthMember authMember = (AuthMember) authentication.getPrincipal();

        String accessToken = Jwts.builder()
                .setSubject(String.valueOf(authMember.getId()))
                .claim("sid", authMember.getUsername())
                .claim("role", authMember.getAuthorities().iterator().next().getAuthority())
                .setExpiration(new Date(System.currentTimeMillis() + JWT_ACCESS_EXPIRED_IN))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        String refreshToken = Jwts.builder()
                .setSubject(String.valueOf(authMember.getId()))
                .claim("sid", authMember.getUsername())
                .claim("role", authMember.getAuthorities().iterator().next().getAuthority())
                .setExpiration(new Date(System.currentTimeMillis() + JWT_REFRESH_EXPIRED_IN))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        return JwtInfo.builder()
                .accessToken(BEARER + accessToken)
                .refreshToken(BEARER + refreshToken)
                .build();
    }

    public boolean validateToken(String token) {
        try {
            Jws<Claims> claims = Jwts.parserBuilder()
                    .setSigningKey(key).build()
                    .parseClaimsJws(token);
            return claims.getBody().getExpiration().after(new Date());
        } catch (ExpiredJwtException e) {
            throw new CustomJwtException(BaseResponseStatus.EXPIRED_ACCESS_TOKEN);
        } catch (UnsupportedJwtException e) {
            throw new CustomJwtException(BaseResponseStatus.UNSUPPORTED_TOKEN_TYPE);
        } catch (SignatureException e) {
            throw new CustomJwtException(BaseResponseStatus.INVALID_SIGNATURE_JWT);
        } catch (MalformedJwtException e) {
            throw new CustomJwtException(BaseResponseStatus.MALFORMED_TOKEN_TYPE);
        } catch (IllegalArgumentException e) {
            throw new CustomJwtException(BaseResponseStatus.INVALID_TOKEN_TYPE);
        } catch (JwtException e) {
            log.error("[JwtTokenProvider.validateAccessToken]", e);
            throw e;
        }
    }

    public Authentication getAuthentication(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key).build()
                .parseClaimsJws(token).getBody();

        Long userId = Long.parseLong(claims.getSubject());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(BaseResponseStatus.USER_NOT_FOUND));

        if (user.getStatus() != BaseStatus.ACTIVE) {
            throw new UserException(BaseResponseStatus.INACTIVE_USER);
        }

        List<GrantedAuthority> authorities = AuthorityUtils.createAuthorityList(user.getUserRole().getRole());

        UserDetails authMember = AuthMember.builder()
                .id(user.getId())
                .username(user.getStudentId())
                .authorities(authorities)
                .status(user.getStatus())
                .build();

        return new UsernamePasswordAuthenticationToken(authMember, "", authorities);
    }

}
