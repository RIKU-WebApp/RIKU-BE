package RIKU.server.Util.Exception;

import RIKU.server.Util.BaseResponseStatus;
import lombok.Getter;

@Getter
public class PostException extends CustomException {
    public PostException(BaseResponseStatus status) { super(status); }
}
