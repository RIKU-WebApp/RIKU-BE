package RIKU.server.Controller.Advice;

import RIKU.server.Util.BaseResponseStatus;
import RIKU.server.Util.Exception.CustomJwtException;
import RIKU.server.Util.Exception.Domain.CustomException;
import RIKU.server.Util.BaseResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    @ExceptionHandler(CustomJwtException.class)
    public ResponseEntity<BaseResponse<Object>> handleCustomJwtException(CustomJwtException e) {
        log.warn("[CustomJwtException] {}", e.getStatus().getResponseMessage());
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)  // 항상 401
                .body(new BaseResponse<>(e.getStatus()));
    }
}
