package RIKU.server.domains.admin.dto.response;

import RIKU.server.domains.user.entity.User;
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
