package RIKU.server.Controller;

import RIKU.server.Dto.User.UserSignUpRequestDto;
import RIKU.server.Service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public String signUp(@RequestBody UserSignUpRequestDto requestDto) {
        userService.signUp(requestDto);
        return ("회원가입이 완료되었습니다.");
    }

}
