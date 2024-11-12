package RIKU.server.Controller;

import RIKU.server.Security.AuthMember;
import RIKU.server.Service.ParticipantService;
import RIKU.server.Util.BaseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/run/post/{postId}")
public class ParticipantController {

    private final ParticipantService participantService;

    @PostMapping("/code")
    public BaseResponse<Map<String, String>> createAttendanceCode(@PathVariable Long postId, @AuthenticationPrincipal AuthMember authMemeber) {
        Long userId = authMemeber.getId();
        String code = participantService.createAttendanceCode(postId, userId);

        Map<String, String> response = new HashMap<>();
        response.put("code", code);

        return new BaseResponse<>(response);
    }

}
