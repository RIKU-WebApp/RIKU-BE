package RIKU.server.domains.run.pacer.dto.response;

import RIKU.server.domains.run.pacer.entity.Pacer;
import lombok.Getter;

@Getter
public class ReadPacersListResponse {
    private String group;

    private String profileImg;

    private String pacerName;
    private Long pacerId;

    private String distance;

    private String pace;

    ReadPacersListResponse(String group, String profileImg, String pacerName, String distance, String pace) {
        this.group = group;
        this.profileImg = profileImg;
        this.pacerName = pacerName;
        this.distance = distance;
        this.pace = pace;
    }

    public static ReadPacersListResponse of(Pacer pacer) {
        var response = new ReadPacersListResponse(
                pacer.getGroup(),
                pacer.getUser().getProfileImageUrl(),
                pacer.getUser().getName(),
                pacer.getDistance(),
                pacer.getPace()
        );
        response.pacerId = pacer.getUser().getId();
        return response;
    }
}
