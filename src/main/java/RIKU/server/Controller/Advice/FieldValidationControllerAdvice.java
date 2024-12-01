package RIKU.server.Controller.Advice;

import RIKU.server.Dto.Common.FieldErrorsDto;
import RIKU.server.Dto.Common.FieldValidationError;
import RIKU.server.Util.BaseResponse;
import RIKU.server.Util.BaseResponseStatus;
import RIKU.server.Util.Exception.Validation.FieldValidationException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static RIKU.server.Util.BaseResponseStatus.INVALID_FIELD;
import static RIKU.server.Util.BaseResponseStatus.METHOD_ARGUMENT_TYPE_MISMATCH;

@Slf4j
@RestControllerAdvice(annotations = RestController.class)
public class FieldValidationControllerAdvice {

    private HttpMessageNotReadableException ex;

    /**
     * @Brief 필드 검증 오류 시 형식에 맞도록 응답 객체 반환
     */
    @ExceptionHandler(FieldValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public BaseResponse<Object> handleValidationException(FieldValidationException ex) {
        log.info("handleValidationException");
        return getFieldErrorsDtos(ex.getBindingResult().getFieldErrors());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public BaseResponse<Object> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex){
        log.info("handleMethodArgumentNotValidException");
        return getFieldErrorsDtos(ex.getBindingResult().getFieldErrors());
    }

    private BaseResponse<Object> getFieldErrorsDtos(List<FieldError> fieldErrors) {
        FieldErrorsDto build = FieldErrorsDto.builder().errors(
                        fieldErrors.stream().map(fieldError ->
                                new FieldValidationError(
                                        fieldError.getField(),
                                        String.valueOf(fieldError.getRejectedValue()),
                                        fieldError.getDefaultMessage())
                        ).toList())
                .build();

        return new BaseResponse<>(INVALID_FIELD, build);
    }


    private static final HashMap<String, String> messageTypeMismatchException = new HashMap<>();
    static {
        messageTypeMismatchException.put("date", "date의 format은 yyyy-MM-dd 입니다.");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public BaseResponse<?> handleTypeMismatchException(MethodArgumentTypeMismatchException ex) {
        log.info("MethodArgumentTypeMismatchException");

        String msg = messageTypeMismatchException.get(ex.getName());
        if(msg == null) msg = ex.getMessage();

        return new BaseResponse<>(METHOD_ARGUMENT_TYPE_MISMATCH,
                new FieldValidationError(ex.getName(), (String) ex.getValue(), msg));
    }

    @ExceptionHandler(InvalidFormatException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public BaseResponse<?> handleHttpMessageNotReadableException(InvalidFormatException ex) {
        log.info("InvalidFormatException");

        FieldErrorsDto build = FieldErrorsDto.builder().errors(
                Collections.singletonList(new FieldValidationError(
                        extractFieldName(ex.getPath().toString()),
                        (String) ex.getValue(),
                        ex.getOriginalMessage()))).build();

        return new BaseResponse<>(INVALID_FIELD, build);
    }

    private static String extractFieldName(String path) {
        int startIndex = path.lastIndexOf("[\"");
        int endIndex = path.lastIndexOf("\"]");
        if (startIndex != -1 && endIndex != -1) {
            return path.substring(startIndex + 2, endIndex);
        }
        return path;
    }
}
