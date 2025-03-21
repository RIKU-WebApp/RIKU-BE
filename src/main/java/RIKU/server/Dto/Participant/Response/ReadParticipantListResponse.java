package RIKU.server.Dto.Participant.Response;

import RIKU.server.Entity.Participant.Participant;
import RIKU.server.Entity.Participant.ParticipantStatus;
import lombok.Getter;

@Getter
public class ReadParticipantListResponse {

    private Long userId;

    private String userName;

    private String userProfileImg;

    private ParticipantStatus status;

    private ReadParticipantListResponse(Long userId, String userName, String userProfileImg, ParticipantStatus status) {
        this.userId = userId;
        this.userName = userName;
        this.userProfileImg = userProfileImg;
        this.status = status;
    }

    public static ReadParticipantListResponse of(Participant participant) {
        return new ReadParticipantListResponse(
                participant.getUser().getId(),
                participant.getUser().getName(),
                participant.getUser().getProfileImageUrl(),
                participant.getParticipantStatus()
        );
    }
}
