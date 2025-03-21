package RIKU.server.Dto.Post.Response;

import RIKU.server.Entity.Board.Pacer;
import lombok.Getter;

@Getter
public class ReadPacersListResponse {
    private String group;

    private String profileImg;

    private String pacerName;

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
        return new ReadPacersListResponse(
                pacer.getGroup(),
                pacer.getUser().getProfileImageUrl(),
                pacer.getUser().getName(),
                pacer.getDistance(),
                pacer.getPace()
        );
    }
}
