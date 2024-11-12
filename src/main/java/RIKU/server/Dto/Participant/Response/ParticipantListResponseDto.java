package RIKU.server.Dto.Participant.Response;

import RIKU.server.Entity.Participant.Participant;
import RIKU.server.Entity.Participant.ParticipantStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ParticipantListResponseDto {
    private Long userId;
    private String userName;
    private ParticipantStatus status;

    public static ParticipantListResponseDto of(Participant participant) {
        return ParticipantListResponseDto.builder()
                .userId(participant.getUser().getId())
                .userName(participant.getUser().getName())
                .status(participant.getStatus())
                .build();

    }
}
