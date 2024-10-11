package RIKU.server.Util.Exception.Validation;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.validation.BindingResult;

@Getter
@Slf4j
public class ValidationException extends RuntimeException {
    private final BindingResult bindingResult;

    public ValidationException(BindingResult bindingResult) {
        this.bindingResult = bindingResult;
        log.info("{} - message : {}", this.getClass().getSimpleName(),
                bindingResult.getAllErrors().stream().map(DefaultMessageSourceResolvable::getDefaultMessage));
    }
}
