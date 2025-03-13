package RIKU.server.Controller;

import RIKU.server.Dto.User.Request.UpdateProfileRequestDto;
import RIKU.server.Dto.User.Request.SignUpUserRequest;
import RIKU.server.Dto.User.Response.ReadUserProfileResponseDto;
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
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/user")
@Tag(name = "User", description = "유저 관련 API")
public class UserController {

    private final UserService userService;

    @Operation(summary = "회원가입", description = """
            
            유저 회원가입을 진행합니다.
            
            """)
    @PostMapping("/signup")
    public BaseResponse<Map<String, Long>> signUp(@Validated @RequestBody SignUpUserRequest requestDto, BindingResult bindingResult) {

        if (bindingResult.hasFieldErrors()) throw new FieldValidationException(bindingResult);

        Long userId = userService.signUp(requestDto);

        Map<String, Long> response = new HashMap<>();
        response.put("userId", userId);

        return new BaseResponse<>(response);
    }

    @Operation(summary = "마이페이지 조회", description = """
            
            유저의 마이페이지를 조회합니다.
            
            """)
    @GetMapping("/profile")
    public BaseResponse<ReadUserProfileResponseDto> getProfile(@AuthenticationPrincipal AuthMember authMember) {
        ReadUserProfileResponseDto responseDto = userService.getProfile(authMember.getId());
        return new BaseResponse<>(responseDto);
    }

    // 마이페이지 수정
    @PutMapping("/profile")
    public BaseResponse<Map<String, Long>> updateProfile(
            @AuthenticationPrincipal AuthMember authMember,
            @ModelAttribute @Validated UpdateProfileRequestDto requestDto,
            BindingResult bindingResult) {

        // 유효성 검증 실패 시 처리
        if (bindingResult.hasFieldErrors()) throw new FieldValidationException(bindingResult);

        Long updatedUserId = userService.updateProfile(authMember.getId(), requestDto);

        Map<String, Long> response = new HashMap<>();
        response.put("userId", updatedUserId);

        return new BaseResponse<>(response);
    }
}
