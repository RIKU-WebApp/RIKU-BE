package RIKU.server.Dto.Post.Request;

import jakarta.annotation.Nullable;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UpdatePacerRequest {

    @Nullable
    private String group;

    @Nullable
    private Long pacerId;

    @Nullable
    private String distance;

    @Nullable
    private String pace;
}
