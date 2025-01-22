package RIKU.server.Security;

import RIKU.server.Util.BaseResponseStatus;
import RIKU.server.Util.Exception.CustomJwtException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtParser {

    public String parseSub(String token) {
        return parseClaimsField(token, "sub", String.class);
    }
    public String parseRole(String token) {
        return parseClaimsField(token, "role", String.class);  // 역할 필드 추가
    }


    /**
     * @Breif 토큰 서명 검증 없이 클레임 필드 파싱
     */
    private  <T> T parseClaimsField(String token, String field, Class<T> tClass) {
        return parseUnsignedClaims(token).get(field, tClass);
    }

    /**
     * @Breif 토큰 서명 검증 없이 클레임 파싱
     */
    private Claims parseUnsignedClaims(String token) {
        try {
            return Jwts.parser()
                    .build()
                    .parseClaimsJwt(getUnsignedToken(token)).getBody();
        } catch (ExpiredJwtException e) {
            throw new CustomJwtException(BaseResponseStatus.EXPIRED_ACCESS_TOKEN);
        }
    }

    /**
     * @Brief 토큰 중 Signature 부분 제거
     */
    private String getUnsignedToken(String token) {
        String[] splitToken = token.split("\\.");
        if (splitToken.length != 3) throw new CustomJwtException(BaseResponseStatus.INVALID_TOKEN_TYPE);
        return splitToken[0] + "." + splitToken[1] + ".";
    }


}
