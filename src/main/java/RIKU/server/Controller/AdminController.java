package RIKU.server.Controller;

import RIKU.server.Dto.User.Request.AuthorizePacerRequest;
import RIKU.server.Dto.User.Request.UpdateUserRoleRequest;
import RIKU.server.Dto.User.Response.ReadPacersResponse;
import RIKU.server.Dto.User.Response.ReadUsersResponse;
import RIKU.server.Security.AuthMember;
import RIKU.server.Service.AdminService;
import RIKU.server.Util.BaseResponse;
import RIKU.server.Util.Exception.Validation.FieldValidationException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("")
@Tag(name = "Admin", description = "운영진 관련 API")
public class AdminController {

    private final AdminService adminService;

    @Operation(summary = "운영진 페이지 부원 조회", description = """
            
            운영진 페이지에 부원들을 전체 조회합니다. (운영진 권한)
            
            """)
    @GetMapping("/admin")
    public BaseResponse<List<ReadUsersResponse>> getUsers(@AuthenticationPrincipal AuthMember authMember) {
        List<ReadUsersResponse> response = adminService.getUsers(authMember);
        return new BaseResponse<>(response);
    }

    @Operation(summary = "부원 등급 변경", description = """
            
            운영진 페이지에서 부원들의 등급을 변경합니다. (운영진 권한)
            
            """)
    @PatchMapping("/admin")
    public BaseResponse<Map<String, Object>> updateUsers(
            @AuthenticationPrincipal AuthMember authMember,
            @Validated @RequestBody List<UpdateUserRoleRequest> request,
            BindingResult bindingResult) {

        if (bindingResult.hasFieldErrors()) throw new FieldValidationException(bindingResult);

        adminService.updateUsers(authMember, request);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "회원 등급 변경이 완료되었습니다.");
        return new BaseResponse<>(response);
    }

    @Operation(summary = "페이서 조회", description = """
            
            정규런 생성 시에 페이서 조회를 합니다.(운영진 권한)
            
            """)
    @GetMapping("/pacers")
    public BaseResponse<List<ReadPacersResponse>> getPacers(@AuthenticationPrincipal AuthMember authMember) {
        List<ReadPacersResponse> response = adminService.getPacers(authMember);
        return new BaseResponse<>(response);
    }

    @Operation(summary = "페이서 업데이트", description = """
            
            운영진이 유저의 페이서 여부를 업데이트 합니다.(운영진 권한)
            
            """)
    @PatchMapping("/pacer")
    public BaseResponse<Map<String, Object>> authorizePacer(
            @AuthenticationPrincipal AuthMember authMember,
            @RequestBody @Validated List<AuthorizePacerRequest> request,
            BindingResult bindingResult) {

        if (bindingResult.hasFieldErrors()) throw new FieldValidationException(bindingResult);

        adminService.authorizePacer(authMember, request);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "페이서 변경이 완료되었습니다.");
        return new BaseResponse<>(response);
    }
}
