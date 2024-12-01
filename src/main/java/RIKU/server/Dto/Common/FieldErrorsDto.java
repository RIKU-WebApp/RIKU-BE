package RIKU.server.Dto.Common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@AllArgsConstructor
@Getter
@Builder
public class FieldErrorsDto {
    List<FieldValidationError> errors;
}
