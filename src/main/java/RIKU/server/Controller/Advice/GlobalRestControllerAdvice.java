package RIKU.server.Controller.Advice;

import RIKU.server.Util.BaseResponseStatus;
import RIKU.server.Util.Exception.Domain.CustomException;
import RIKU.server.Util.BaseResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@Slf4j
@RestControllerAdvice
public class GlobalRestControllerAdvice {

    @ExceptionHandler(CustomException.class)
    public BaseResponse<Object> handleCustomException (CustomException e) {
        return new BaseResponse<>(e.getStatus());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public BaseResponse<Object> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        log.error("[MaxUploadSizeExceededException] 요청 크기 초과: {}", e.getMessage());
        return new BaseResponse<>(BaseResponseStatus.REQUEST_SIZE_EXCEEDED);
    }
}
