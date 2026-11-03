package RIKU.server.global.validation;

import org.springframework.validation.BindingResult;

public class FieldValidationException extends ValidationException {
    public FieldValidationException(BindingResult bindingResult) { super(bindingResult); }
}
