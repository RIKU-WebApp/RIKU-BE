package RIKU.server.Controller;

import RIKU.server.Dto.User.Request.UserLoginRequestDto;
import RIKU.server.Dto.User.Response.UserLoginResponseDto;
import RIKU.server.Dto.User.Response.UserRoleResponseDto;
import RIKU.server.Entity.User.UserRole;
import RIKU.server.Service.AuthService;
import RIKU.server.Util.BaseResponse;
import RIKU.server.Util.BaseResponseStatus;
import RIKU.server.Util.Exception.Domain.UserException;
import RIKU.server.Util.Exception.Validation.FieldValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class AuthController {
    private final AuthService authService;

    // 로그인
    @PostMapping("/login")
    public BaseResponse<UserLoginResponseDto> login(@Validated @RequestBody UserLoginRequestDto requestDto, BindingResult bindingResult) {
        if(bindingResult.hasFieldErrors()) throw new FieldValidationException(bindingResult);

        UserLoginResponseDto response = authService.login(requestDto);

        return new BaseResponse<>(response);
    }

    // 멤버 등급 업데이트
    @PutMapping("/{userId}/role")
    public BaseResponse<UserRoleResponseDto> updateUserRole(
            @PathVariable Long userId,
            @RequestBody String userRole) {
        try {
            UserRole newRole = UserRole.valueOf(userRole.trim());
            UserRoleResponseDto response = authService.updateUserRole(userId, newRole);

            return new BaseResponse<>(response);
        } catch (IllegalArgumentException e) {
            throw new UserException(BaseResponseStatus.INVALID_USER_ROLE);
        }
    }

    // 학번 중복 확인
    @GetMapping("/check-id")
    public BaseResponse<Boolean> checkStudentIdDuplicate(@RequestParam String studentId) {
        boolean isDuplicate = authService.checkStudentIdDuplicate(studentId);
        return new BaseResponse<>(isDuplicate);
    }
}
