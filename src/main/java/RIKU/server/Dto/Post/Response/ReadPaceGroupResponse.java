package RIKU.server.Dto.Post.Response;

import RIKU.server.Entity.Board.Pacer;
import lombok.Getter;

@Getter
public class ReadPaceGroupResponse {

    private String group;

    private String pace;

    private ReadPaceGroupResponse(String group, String pace) {
        this.group = group;
        this.pace = pace;
    }

    public static ReadPaceGroupResponse of(Pacer pacer) {
        return new ReadPaceGroupResponse(pacer.getGroup(), pacer.getPace());
    }
}
