package RIKU.server.Dto.User.Request;

import RIKU.server.Entity.User.UserRole;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UpdateUsersRequest {

    @NotBlank(message = "학번은 필수입니다")
    private String studentId;

    @Nullable
    private UserRole userRole;

    @Nullable
    private Boolean isPacer;
}