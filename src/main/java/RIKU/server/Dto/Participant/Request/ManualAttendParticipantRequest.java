package RIKU.server.Dto.Participant.Request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ManualAttendParticipantRequest {

    @NotNull
    private Long userId;

    @NotNull
    private Boolean isAttend;
}
