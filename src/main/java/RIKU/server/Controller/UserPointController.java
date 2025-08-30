package RIKU.server.Controller;

import RIKU.server.Dto.User.Request.ReadUserEventRankingRequest;
import RIKU.server.Dto.User.Response.ReadUserParticipationsResponse;
import RIKU.server.Dto.User.Response.ReadUserRankingResponse;
import RIKU.server.Security.AuthMember;
import RIKU.server.Service.UserPointService;
import RIKU.server.Util.BaseResponse;
import RIKU.server.Util.Exception.Validation.FieldValidationException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


@RestController
@RequiredArgsConstructor
@RequestMapping("")
@Tag(name = "UserPoint", description = "유저 포인트 관련 API")
public class UserPointController {

    private final UserPointService userPointService;

    @Operation(summary = " 전체 유저 포인트 랭킹 조회", description = """
            
            현재 로그인한 유저의 전체 포인트 랭킹을 포함한 상위 20명의 랭킹 정보를 반환합니다.(시즌제)
            
            """)
    @GetMapping("/ranking")
    public BaseResponse<ReadUserRankingResponse> getUserPointRanking(@AuthenticationPrincipal AuthMember authMember) {
        ReadUserRankingResponse response = userPointService.getUserPointRanking(authMember);
        return new BaseResponse<>(response);
    }

    @Operation(summary = "마이페이지 활동내역 조회", description = """
            
            로그인한 유저의 포인트 내역, 참여 내역, 누적 포인트, 랭킹을 반환합니다.(시즌제)
            
            """)
    @GetMapping("/user/profile/participations")
    public BaseResponse<ReadUserParticipationsResponse> getParticipations(@AuthenticationPrincipal AuthMember authMember) {
        ReadUserParticipationsResponse response = userPointService.getParticipations(authMember);
        return new BaseResponse<>(response);
    }

    @Operation(summary = "이벤트 포인트 랭킹 조회", description = """
            
            기간과 포인트 타입을 기준으로 유저들의 포인트 합계를 계산해 랭킹을 반환합니다.
            클라이언트는 startDate, endDate를 KST 기준으로 보내야합니다.
            
            """)
    @PostMapping("/ranking/event")
    public BaseResponse<ReadUserRankingResponse> getUserEventPointRanking(
            @Validated @RequestBody ReadUserEventRankingRequest request,
            BindingResult bindingResult,
            @AuthenticationPrincipal AuthMember authMember) {
        if (bindingResult.hasFieldErrors()) throw new FieldValidationException(bindingResult);
        ReadUserRankingResponse response = userPointService.getUserEventPointRanking(request, authMember);
        return new BaseResponse<>(response);
    }
}
