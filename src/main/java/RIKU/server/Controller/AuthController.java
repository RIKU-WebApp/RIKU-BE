package RIKU.server.Controller;

import RIKU.server.Dto.User.Request.LoginUserRequest;
import RIKU.server.Dto.User.Response.LoginUserResponse;
import RIKU.server.Dto.User.Response.UpdateUserRoleResponse;
import RIKU.server.Entity.User.UserRole;
import RIKU.server.Service.AuthService;
import RIKU.server.Util.BaseResponse;
import RIKU.server.Util.BaseResponseStatus;
import RIKU.server.Util.Exception.Domain.UserException;
import RIKU.server.Util.Exception.Validation.FieldValidationException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


@RestController
@RequiredArgsConstructor
@RequestMapping("/user")
@Tag(name = "Auth", description = "인가 인증 관련 API")
public class AuthController {
    private final AuthService authService;

    @Operation(summary = "로그인", description = """
            
            유저 로그인을 진행합니다.
            
            """)
    @PostMapping("/login")
    public BaseResponse<LoginUserResponse> login(@Validated @RequestBody LoginUserRequest request, BindingResult bindingResult) {
        if(bindingResult.hasFieldErrors()) throw new FieldValidationException(bindingResult);

        LoginUserResponse response = authService.login(request);

        return new BaseResponse<>(response);
    }

    @Operation(summary = "학번 중복 확인", description = """
            
            회원가입 시, 유저가 입력한 학번을 중복 확인 합니다.
            
            """)
    @GetMapping("/check-id")
    public BaseResponse<Boolean> checkStudentIdDuplicate(@RequestParam String studentId) {
        boolean isDuplicate = authService.checkStudentIdDuplicate(studentId);
        return new BaseResponse<>(isDuplicate);
    }
}
