package RIKU.server.Dto.Participant.Response;

import lombok.Getter;

import java.util.List;

@Getter
public class GroupParticipantResponse {

    private String group;

    private List<ReadParticipantListResponse> participants;

    private GroupParticipantResponse(String group, List<ReadParticipantListResponse> participants) {
        this.group = group;
        this.participants = participants;
    }

    public static GroupParticipantResponse of(String group, List<ReadParticipantListResponse> participants) {
        return new GroupParticipantResponse(group, participants);
    }
}
