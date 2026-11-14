package RIKU.server.global.validation;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.FieldError;
import org.springframework.validation.BindingResult;

@Getter
@Slf4j
public class ValidationException extends RuntimeException {
    private final BindingResult bindingResult;

    public ValidationException(BindingResult bindingResult) {
        this.bindingResult = bindingResult;
        log.info("{} fields={}", this.getClass().getSimpleName(),
                bindingResult.getFieldErrors().stream().map(FieldError::getField).distinct().toList());
    }
}
