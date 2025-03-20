package RIKU.server.Dto.Participant.Response;

import RIKU.server.Entity.Participant.Participant;
import RIKU.server.Entity.Participant.ParticipantStatus;
import lombok.Getter;

@Getter
public class UpdateParticipantResponse {

    private Long userId;

    private Long postId;

    private ParticipantStatus status;

    private UpdateParticipantResponse(Long userId, Long postId, ParticipantStatus status) {
        this.userId = userId;
        this.postId = postId;
        this.status = status;
    }
    public static UpdateParticipantResponse of (Participant participant) {
        return new UpdateParticipantResponse(
                participant.getUser().getId(),
                participant.getPost().getId(),
                participant.getStatus()
        );
    }
}
