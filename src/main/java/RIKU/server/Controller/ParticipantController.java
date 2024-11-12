package RIKU.server.Controller;

import RIKU.server.Dto.Participant.Request.ParticipantRequestDto;
import RIKU.server.Dto.Participant.Response.ParticipantResponseDto;
import RIKU.server.Security.AuthMember;
import RIKU.server.Service.ParticipantService;
import RIKU.server.Util.BaseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/run/post/{postId}")
public class ParticipantController {

    private final ParticipantService participantService;

    // 출석 코드 생성
    @PostMapping("/code")
    public BaseResponse<Map<String, String>> createAttendanceCode(@PathVariable Long postId, @AuthenticationPrincipal AuthMember authMemeber) {
        Long userId = authMemeber.getId();
        String code = participantService.createAttendanceCode(postId, userId);

        Map<String, String> response = new HashMap<>();
        response.put("code", code);

        return new BaseResponse<>(response);
    }

    // 참여하기
    @PostMapping("/join")
    public BaseResponse<ParticipantResponseDto> joinRun(@PathVariable Long postId, @AuthenticationPrincipal AuthMember authMember) {
        Long userId = authMember.getId();
        ParticipantResponseDto responseDto = participantService.joinRun(postId, userId);
        return new BaseResponse<>(responseDto);
    }

    // 출석하기
    @PostMapping("/attend")
    public BaseResponse<ParticipantResponseDto> attendRun(
            @PathVariable Long postId,
            @AuthenticationPrincipal AuthMember authMember,
            @RequestBody ParticipantRequestDto requestDto) {
        Long userId = authMember.getId();
        String code = requestDto.getCode();
        ParticipantResponseDto responseDto = participantService.attendRun(postId, userId, code);
        return new BaseResponse<>(responseDto);
    }

}
