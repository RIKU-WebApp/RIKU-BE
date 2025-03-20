package RIKU.server.Controller;

import RIKU.server.Dto.Participant.Request.AttendParticipantRequest;
import RIKU.server.Dto.Participant.Response.UpdateParticipantResponse;
import RIKU.server.Security.AuthMember;
import RIKU.server.Service.ParticipantService;
import RIKU.server.Util.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/run/{runType}/post/{postId}")
@Tag(name = "Participant", description = "참여/출석 관련 API")
public class ParticipantController {

    private final ParticipantService participantService;

    @Operation(summary = "출석 코드 생성", description = """
            
            생성자가 러닝을 시작합니다.(번개런은 생성자 권한, 나머지는 운영진 권한)
            
            """)
    @PostMapping("/code")
    public BaseResponse<Map<String, String>> createAttendanceCode(
            @PathVariable String runType,
            @PathVariable Long postId,
            @AuthenticationPrincipal AuthMember authMember) {
        String code = participantService.createAttendanceCode(runType, postId, authMember);

        Map<String, String> response = new HashMap<>();
        response.put("code", code);

        return new BaseResponse<>(response);
    }

    @Operation(summary = "참여하기", description = """
            
            유저가 러닝에 참여합니다.
            
            """)
    @PostMapping("/join")
    public BaseResponse<UpdateParticipantResponse> joinRun(
            @PathVariable String runType,
            @PathVariable Long postId,
            @AuthenticationPrincipal AuthMember authMember) {
        UpdateParticipantResponse response = participantService.joinRun(runType, postId, authMember);
        return new BaseResponse<>(response);
    }

    @Operation(summary = "출석하기", description = """
            
            유저가 러닝에 출석합니다.(출석코드 입력)
            
            """)
    @PostMapping("/attend")
    public BaseResponse<UpdateParticipantResponse> attendRun(
            @PathVariable String runType,
            @PathVariable Long postId,
            @AuthenticationPrincipal AuthMember authMember,
            @RequestBody AttendParticipantRequest request) {
        String code = request.getCode();
        UpdateParticipantResponse response = participantService.attendRun(runType, postId, authMember, code);
        return new BaseResponse<>(response);
    }

    // 출석 종료하기
    @PutMapping("/close")
    public BaseResponse<Map<String, Object>> closeRun(
            @PathVariable Long postId,
            @AuthenticationPrincipal AuthMember authMember) {
        Long userId = authMember.getId();
        String postStatus = participantService.closeRun(postId, userId);

        Map<String, Object> response = new HashMap<>();
        response.put("postStatus", postStatus);
        response.put("postId", postId);

        return new BaseResponse<>(response);
    }

}
