package RIKU.server.domains.run.post.exception;

import RIKU.server.global.exception.CustomException;

import RIKU.server.global.response.BaseResponseStatus;
import lombok.Getter;

@Getter
public class PostException extends CustomException {
    public PostException(BaseResponseStatus status) { super(status); }
}
