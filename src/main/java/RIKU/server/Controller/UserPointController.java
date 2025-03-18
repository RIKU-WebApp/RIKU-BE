package RIKU.server.Controller;

import RIKU.server.Dto.User.Response.UserRankingResponseDto;
import RIKU.server.Security.AuthMember;
import RIKU.server.Service.UserPointService;
import RIKU.server.Util.BaseResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequiredArgsConstructor
@RequestMapping("")
@Tag(name = "UserPoint", description = "유저 포인트 관련 API")
public class UserPointController {
    private final UserPointService userPointService;

    @GetMapping("/ranking")
    public BaseResponse<UserRankingResponseDto> getUserPointRanking(@AuthenticationPrincipal AuthMember authMember) {
        UserRankingResponseDto response = userPointService.getUserPointRanking(authMember.getId());
        return new BaseResponse<>(response);
    }
}
