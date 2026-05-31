package RIKU.server.domains.run.participant.dto.response;

import RIKU.server.domains.run.participant.entity.Participant;
import RIKU.server.domains.run.participant.entity.ParticipantStatus;
import lombok.Getter;

@Getter
public class ReadParticipantListResponse {

    private Long userId;

    private String userName;

    private String userProfileImg;

    private ParticipantStatus status;

    private boolean isPacer;


    private ReadParticipantListResponse(Long userId, String userName, String userProfileImg, ParticipantStatus status, boolean isPacer) {
        this.userId = userId;
        this.userName = userName;
        this.userProfileImg = userProfileImg;
        this.status = status;
        this.isPacer = isPacer;
    }

    public static ReadParticipantListResponse of(Participant participant) {
        return new ReadParticipantListResponse(
                participant.getUser().getId(),
                participant.getUser().getName(),
                participant.getUser().getProfileImageUrl(),
                participant.getParticipantStatus(),
                participant.getUser().getIsPacer()
        );
    }
}
