package RIKU.server.Util.Exception;

import RIKU.server.Util.BaseResponseStatus;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
public class CustomException extends RuntimeException {
    private BaseResponseStatus status;

    public CustomException(BaseResponseStatus status) {
        super(status.getResponseMessage());
        this.status = status;
        log.info("{} - message : {}", this.getClass().getSimpleName(), status.getResponseMessage());
    }
}
