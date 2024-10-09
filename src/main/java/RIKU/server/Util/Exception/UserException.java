package RIKU.server.Util.Exception;

import RIKU.server.Util.BaseResponseStatus;
import lombok.Getter;

@Getter
public class UserException extends CustomException {
    public UserException(BaseResponseStatus status) { super(status); }
}
