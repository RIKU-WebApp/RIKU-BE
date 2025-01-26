package RIKU.server.Controller;

import RIKU.server.Dto.User.Request.UserLoginRequestDto;
import RIKU.server.Dto.User.Response.UserLoginResponseDto;
import RIKU.server.Dto.User.Response.UserRoleResponseDto;
import RIKU.server.Entity.User.UserRole;
import RIKU.server.Service.AuthService;
import RIKU.server.Util.BaseResponse;
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

    @PostMapping("/login")
    public BaseResponse<UserLoginResponseDto> login(@Validated @RequestBody UserLoginRequestDto requestDto, BindingResult bindingResult) {
        if(bindingResult.hasFieldErrors()) throw new FieldValidationException(bindingResult);

        UserLoginResponseDto response = authService.login(requestDto);

        return new BaseResponse<>(response);
    }

    @PutMapping("/{userId}/role")
    public BaseResponse<UserRoleResponseDto> updateUserRole(@PathVariable Long userId, @RequestBody String userRole) {
        UserRole newRole = UserRole.valueOf(userRole);
        UserRoleResponseDto response = authService.updateUserRole(userId, newRole);

        return new BaseResponse<>(response);

    }
}
