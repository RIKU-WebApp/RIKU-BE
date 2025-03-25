package RIKU.server.Controller;

import RIKU.server.Dto.User.Request.UpdateProfileRequest;
import RIKU.server.Dto.User.Request.SignUpUserRequest;
import RIKU.server.Dto.User.Response.ReadUserProfileResponse;
import RIKU.server.Security.AuthMember;
import RIKU.server.Service.UserService;
import RIKU.server.Util.BaseResponse;
import RIKU.server.Util.Exception.Validation.FieldValidationException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("")
@Tag(name = "User", description = "유저 관련 API")
public class UserController {

    private final UserService userService;

    @Operation(summary = "회원가입", description = """
            
            유저 회원가입을 진행합니다.
            
            """)
    @PostMapping("/user/signup")
    public BaseResponse<Map<String, Long>> signUp(@Validated @RequestBody SignUpUserRequest request, BindingResult bindingResult) {
        if (bindingResult.hasFieldErrors()) throw new FieldValidationException(bindingResult);

        Long userId = userService.signUp(request);

        Map<String, Long> response = new HashMap<>();
        response.put("userId", userId);

        return new BaseResponse<>(response);
    }

    @Operation(summary = "마이페이지 조회", description = """
            
            유저의 마이페이지를 조회합니다.
            
            """)
    @GetMapping("/user/profile")
    public BaseResponse<ReadUserProfileResponse> getProfile(@AuthenticationPrincipal AuthMember authMember) {
        ReadUserProfileResponse response = userService.getProfile(authMember.getId());
        return new BaseResponse<>(response);
    }

    @Operation(summary = "마이페이지 수정", description = """
            
            유저가 마이페이지를 수정합니다.
            
            """)
    @PatchMapping("/user/profile")
    public BaseResponse<Map<String, Long>> updateProfile(
            @AuthenticationPrincipal AuthMember authMember,
            @ModelAttribute @Validated UpdateProfileRequest request,
            BindingResult bindingResult) {
        if (bindingResult.hasFieldErrors()) throw new FieldValidationException(bindingResult);

        Long updatedUserId = userService.updateProfile(authMember.getId(), request);

        Map<String, Long> response = new HashMap<>();
        response.put("userId", updatedUserId);

        return new BaseResponse<>(response);
    }

    @Operation(summary = "마이페이지 출석", description = """
            
            유저가 마이페이지에서 오늘의 출석을 합니다.
            
            """)
    @PostMapping("/user/attend")
    public BaseResponse<Map<String, Object>> attendProfile(@AuthenticationPrincipal AuthMember authMember) {
        userService.attendProfile(authMember);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "출석이 완료되었습니다.");
        return new BaseResponse<>(response);
    }
}
