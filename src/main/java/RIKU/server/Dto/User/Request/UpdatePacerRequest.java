package RIKU.server.Dto.User.Request;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UpdatePacerRequest {

    private String studentId;

    private Boolean isPacer;
}
