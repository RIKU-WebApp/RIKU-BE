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

    @Value("${secret.jwt-issuer:riku-local}")
    private String issuer;

    private final Key key;

    private final UserRepository userRepository;

    public static final String BEARER = "Bearer ";

    public JwtTokenProvider(@Value("${secret.jwt-secret-key}") String JWT_SECRET_KEY, UserRepository userRepository) {
        this.userRepository = userRepository;
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(JWT_SECRET_KEY));
    }

    public JwtInfo createToken(Authentication authentication) {
        AuthMember authMember = (AuthMember) authentication.getPrincipal();

        User user = userRepository.findById(authMember.getId())
                .orElseThrow(() -> new UserException(BaseResponseStatus.USER_NOT_FOUND));
        String version = credentialVersion(user.getPassword());
        String accessToken = Jwts.builder()
                .setIssuer(issuer).setAudience("riku-api").claim("purpose", "access")
                .claim("credentialVersion", version).setIssuedAt(new Date())
                .setSubject(String.valueOf(authMember.getId()))
                .claim("sid", authMember.getUsername())
                .claim("role", authMember.getAuthorities().iterator().next().getAuthority())
                .setExpiration(new Date(System.currentTimeMillis() + JWT_ACCESS_EXPIRED_IN))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        String refreshToken = Jwts.builder()
                .setIssuer(issuer).setAudience("riku-api").claim("purpose", "refresh")
                .claim("credentialVersion", version).setIssuedAt(new Date())
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
            parseAccess(token);
            return true;
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
            throw new CustomJwtException(BaseResponseStatus.INVALID_TOKEN_TYPE);
        }
    }

    public Authentication getAuthentication(String token) {
        Claims claims = parseAccess(token);
        Long userId;
        try { userId = Long.parseLong(claims.getSubject()); }
        catch (RuntimeException invalid) { throw new CustomJwtException(BaseResponseStatus.INVALID_TOKEN_TYPE); }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(BaseResponseStatus.USER_NOT_FOUND));

        if (user.getStatus() != BaseStatus.ACTIVE) {
            throw new UserException(BaseResponseStatus.INACTIVE_USER);
        }

        if (!java.util.Objects.equals(user.getStudentId(), claims.get("sid", String.class))
                || !java.util.Objects.equals(credentialVersion(user.getPassword()), claims.get("credentialVersion", String.class))) {
            throw new CustomJwtException(BaseResponseStatus.INVALID_TOKEN_TYPE);
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


    private Claims parseAccess(String token) {
        Claims claims = Jwts.parserBuilder().setSigningKey(key)
                .requireIssuer(issuer).requireAudience("riku-api").require("purpose", "access")
                .build().parseClaimsJws(token).getBody();
        if (claims.getExpiration() == null || claims.getSubject() == null || claims.getIssuedAt() == null) {
            throw new CustomJwtException(BaseResponseStatus.INVALID_TOKEN_TYPE);
        }
        return claims;
    }

    private String credentialVersion(String passwordHash) {
        try {
            byte[] digest = java.security.MessageDigest.getInstance("SHA-256")
                    .digest(passwordHash.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.Base64.getEncoder().encodeToString(digest);
        } catch (java.security.NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }
}
