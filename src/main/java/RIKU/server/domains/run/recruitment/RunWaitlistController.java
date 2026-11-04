package RIKU.server.domains.run.recruitment;

import RIKU.server.global.response.BaseResponse;
import RIKU.server.security.AuthMember;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/run/{type:flash|training|event}/post/{id}/waitlist")
public class RunWaitlistController {
    private final RunWaitlist waitlist;

    @GetMapping
    public BaseResponse<RunWaitlist.View> read(@PathVariable String type, @PathVariable Long id,
            @AuthenticationPrincipal AuthMember member) {
        return new BaseResponse<>(waitlist.read(type, id, member.getId()));
    }

    @DeleteMapping("/me")
    public BaseResponse<Boolean> leave(@PathVariable String type, @PathVariable Long id,
            @AuthenticationPrincipal AuthMember member) {
        waitlist.leave(type, id, member.getId());
        return new BaseResponse<>(true);
    }
}
