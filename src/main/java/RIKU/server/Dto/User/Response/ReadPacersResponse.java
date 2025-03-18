package RIKU.server.Dto.User.Response;

import RIKU.server.Entity.User.User;
import lombok.Getter;

@Getter
public class ReadPacersResponse {

    private Long id;

    private String name;

    private ReadPacersResponse(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    public static ReadPacersResponse of(User user) {
        return new ReadPacersResponse(user.getId(), user.getName());
    }
}
