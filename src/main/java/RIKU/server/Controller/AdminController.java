package RIKU.server.Controller;

import RIKU.server.Dto.User.Response.ReadUsersResponseDto;
import RIKU.server.Security.AuthMember;
import RIKU.server.Service.AdminService;
import RIKU.server.Util.BaseResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

}
