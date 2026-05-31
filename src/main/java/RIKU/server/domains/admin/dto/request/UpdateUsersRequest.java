package RIKU.server.domains.admin.dto.request;

import RIKU.server.domains.user.entity.UserRole;
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
    private String userRole;

    @Nullable
    private Boolean isPacer;
}