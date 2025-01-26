package RIKU.server.Controller;

import RIKU.server.Dto.User.UserRoleDto;
import RIKU.server.Dto.User.Response.ReadUsersResponseDto;
import RIKU.server.Security.AuthMember;
import RIKU.server.Service.AdminService;
import RIKU.server.Util.BaseResponse;
import RIKU.server.Util.Exception.Validation.FieldValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/admin")
public class AdminController {

    private final AdminService adminService;

    // 운영진 페이지 (부원 조회)
    @GetMapping("")
    public BaseResponse<List<ReadUsersResponseDto>> getUsers(@AuthenticationPrincipal AuthMember authMember) {
        List<ReadUsersResponseDto> response = adminService.getUsers(authMember);
        return new BaseResponse<>(response);
    }

    // 회원 등급 변경
    @PutMapping("")
    public BaseResponse<List<UserRoleDto>> updateUsers(
            @AuthenticationPrincipal AuthMember authMember,
            @Validated @RequestBody List<UserRoleDto> requestDto,
            BindingResult bindingResult) {

        if (bindingResult.hasFieldErrors()) throw new FieldValidationException(bindingResult);

        List<UserRoleDto> response = adminService.updateUsers(authMember, requestDto);
        return new BaseResponse<>(response);
    }
}
