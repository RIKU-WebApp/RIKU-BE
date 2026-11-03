package RIKU.server.domains.run.participant.dto.response;

import RIKU.server.domains.run.participant.entity.Participant;
import RIKU.server.domains.run.participant.entity.ParticipantStatus;
import lombok.Getter;

@Getter
public class UpdateParticipantResponse {

    private Long userId;

    private Long postId;

    private ParticipantStatus status;

    private String message;

    private UpdateParticipantResponse(Long userId, Long postId, ParticipantStatus status, String message) {
        this.userId = userId;
        this.postId = postId;
        this.status = status;
        this.message = message;
    }
    public static UpdateParticipantResponse of (Participant participant) {
        return new UpdateParticipantResponse(
                participant.getUser().getId(),
                participant.getPost().getId(),
                participant.getParticipantStatus(),
                null
        );
    }

    public static UpdateParticipantResponse message (String message) {
        return new UpdateParticipantResponse(null, null, null, message);
    }
}
