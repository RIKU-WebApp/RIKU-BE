package RIKU.server.Dto.Common;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class FieldValidationError {
    private String fieldName;
    private String rejectValue;
    private String message;
}
