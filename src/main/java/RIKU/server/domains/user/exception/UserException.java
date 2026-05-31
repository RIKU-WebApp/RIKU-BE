package RIKU.server.domains.user.exception;

import RIKU.server.global.exception.CustomException;

import RIKU.server.global.response.BaseResponseStatus;
import lombok.Getter;

@Getter
public class UserException extends CustomException {
    public UserException(BaseResponseStatus status) { super(status); }
}
