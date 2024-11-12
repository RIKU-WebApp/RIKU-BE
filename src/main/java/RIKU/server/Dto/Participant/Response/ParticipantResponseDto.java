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
public class ParticipantResponseDto {
    private Long userId;
    private Long postId;
    private ParticipantStatus status;

    public static ParticipantResponseDto of (Participant participant) {
        return ParticipantResponseDto.builder()
                .userId(participant.getUser().getId())
                .postId(participant.getPost().getId())
                .status(participant.getStatus())
                .build();
    }
}
