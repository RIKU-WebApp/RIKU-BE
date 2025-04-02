package RIKU.server.Dto.Post.Request;

import RIKU.server.Entity.Board.Pacer;
import RIKU.server.Entity.Board.Post.Post;
import RIKU.server.Entity.User.User;
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
