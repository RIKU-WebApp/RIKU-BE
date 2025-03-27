package RIKU.server.Dto.Participant.Request;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ManualAttendParticipantRequest {

    private Long userId;

    private Boolean isAttend;
}
