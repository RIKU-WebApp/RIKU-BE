package RIKU.server.Controller;

import RIKU.server.Dto.User.Request.UpdateProfileRequestDto;
import RIKU.server.Dto.User.Request.UserSignUpRequestDto;
import RIKU.server.Dto.User.Response.ReadUserProfileResponseDto;
import RIKU.server.Security.AuthMember;
import RIKU.server.Service.UserService;
import RIKU.server.Util.BaseResponse;
import RIKU.server.Util.Exception.Validation.FieldValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    // 회원가입
    @PostMapping("/signup")
    public BaseResponse<Map<String, Long>> signUp(@Validated @RequestBody UserSignUpRequestDto requestDto, BindingResult bindingResult) {
        if (bindingResult.hasFieldErrors()) throw new FieldValidationException(bindingResult);
        Long userId = userService.signUp(requestDto);

        Map<String, Long> response = new HashMap<>();
        response.put("userId", userId);

        return new BaseResponse<>(response);
    }

    // 마이페이지 조회
    @GetMapping("/profile")
    public BaseResponse<ReadUserProfileResponseDto> getProfile(@AuthenticationPrincipal AuthMember authMember) {
        ReadUserProfileResponseDto responseDto = userService.getProfile(authMember.getId());
        return new BaseResponse<>(responseDto);
    }

    // 마이페이지 수정
    @PutMapping("/profile")
    public BaseResponse<Map<String, Long>> updateProfile(
            @AuthenticationPrincipal AuthMember authMember,
            @Validated @RequestBody UpdateProfileRequestDto requestDto,
            BindingResult bindingResult) {
        if (bindingResult.hasFieldErrors()) throw new FieldValidationException(bindingResult);

        Long userId = userService.updateProfile(authMember.getId(), requestDto);

        Map<String, Long> response = new HashMap<>();
        response.put("userId", userId);

        return new BaseResponse<>(response);
    }
}
