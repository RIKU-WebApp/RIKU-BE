package RIKU.server.global.validation.dto;

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
