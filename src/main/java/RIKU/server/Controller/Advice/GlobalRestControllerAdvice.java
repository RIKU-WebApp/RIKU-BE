package RIKU.server.Controller.Advice;

import RIKU.server.Util.Exception.Domain.CustomException;
import RIKU.server.Util.BaseResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalRestControllerAdvice {

    @ExceptionHandler(CustomException.class)
    public BaseResponse<Object> handleCustomException (CustomException e) {
        return new BaseResponse<>(e.getStatus());
    }
}
