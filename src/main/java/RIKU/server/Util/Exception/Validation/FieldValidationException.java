package RIKU.server.Util.Exception.Validation;

import org.springframework.validation.BindingResult;

public class FieldValidationException extends ValidationException {
    public FieldValidationException(BindingResult bindingResult) { super(bindingResult); }
}
