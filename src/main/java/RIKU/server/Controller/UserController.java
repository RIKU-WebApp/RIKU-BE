package RIKU.server.Controller;

import RIKU.server.Dto.User.Request.UserLoginRequestDto;
import RIKU.server.Dto.User.Request.UserSignUpRequestDto;
import RIKU.server.Dto.User.Response.UserLoginResponseDto;
import RIKU.server.Service.UserService;
import RIKU.server.Util.BaseResponse;
import RIKU.server.Util.Exception.Validation.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    /**
     * 회원가입
     */
    @PostMapping("/signup")
    public BaseResponse<Map<String, Long>> signUp(@Validated @RequestBody UserSignUpRequestDto requestDto, BindingResult bindingResult) {
        if (bindingResult.hasFieldErrors()) throw new ValidationException(bindingResult);
        Long userId = userService.signUp(requestDto);

        Map<String, Long> response = new HashMap<>();
        response.put("userId", userId);

        return new BaseResponse<>(response);
    }



}
