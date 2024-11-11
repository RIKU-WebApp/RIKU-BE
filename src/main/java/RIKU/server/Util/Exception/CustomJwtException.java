package RIKU.server.Util.Exception;

import RIKU.server.Util.BaseResponseStatus;
import io.jsonwebtoken.JwtException;
import lombok.Getter;

@Getter
public class CustomJwtException extends JwtException {

    private final BaseResponseStatus status;

    public CustomJwtException(BaseResponseStatus status) {
        super(status.getResponseMessage());
        this.status = status;
    }
}