package RIKU.server.Dto.Post.Request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
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
}
