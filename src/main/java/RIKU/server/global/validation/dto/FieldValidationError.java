package RIKU.server.global.validation.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class FieldValidationError {
    private String fieldName;
    private String rejectValue;
    private String message;
}
