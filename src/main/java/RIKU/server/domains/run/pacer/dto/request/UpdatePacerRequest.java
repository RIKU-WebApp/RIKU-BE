package RIKU.server.domains.run.pacer.dto.request;

import RIKU.server.domains.run.pacer.entity.Pacer;
import RIKU.server.domains.run.post.entity.Post;
import RIKU.server.domains.user.entity.User;
import jakarta.annotation.Nullable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
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

    public Pacer toEntity(User user, Post post) {
        return Pacer.create(
                user,
                post,
                this.getGroup(),
                this.getPace(),
                this.getDistance()
        );
    }
}
