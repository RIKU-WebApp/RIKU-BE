package RIKU.server.Controller;

import RIKU.server.Dto.User.Response.UserRankingResponseDto;
import RIKU.server.Security.AuthMember;
import RIKU.server.Service.UserPointService;
import RIKU.server.Util.BaseResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@Slf4j
@RequiredArgsConstructor
public class UserPointController {
    private final UserPointService userPointService;

    @GetMapping("/ranking")
    public BaseResponse<UserRankingResponseDto> getUserPointRanking(@AuthenticationPrincipal AuthMember authMember) {
        UserRankingResponseDto response = userPointService.getUserPointRanking(authMember.getId());
        return new BaseResponse<>(response);
    }
}
