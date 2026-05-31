package RIKU.server.domains.admin.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AuthorizePacerRequest {

    private String studentId;

    private Boolean isPacer;
}
