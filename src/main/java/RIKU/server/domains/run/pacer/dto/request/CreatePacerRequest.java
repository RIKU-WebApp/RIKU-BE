package RIKU.server.domains.run.pacer.dto.request;

import RIKU.server.domains.run.pacer.entity.Pacer;
import RIKU.server.domains.run.post.entity.Post;
import RIKU.server.domains.user.entity.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CreatePacerRequest {

    @NotBlank(message = "그룹은 필수 항목입니다.")
    private String group;

    @NotNull(message = "페이서 아이디는 필수 항목입니다.")
    private Long pacerId;

    @NotBlank(message = "거리는 필수 항목입니다.")
    private String distance;

    @NotBlank(message = "페이스는 필수 항목입니다.")
    private String pace;

    public Pacer toEntity(User user, Post post){
        return Pacer.create(
                user,
                post,
                this.getGroup(),
                this.getPace(),
                this.getDistance()
        );
    }
}
