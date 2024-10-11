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

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    /**
     * 회원가입
     * @param requestDto
     * @param bindingResult
     * @return
     */
    @PostMapping("/signup")
    public BaseResponse<Object> signUp(@Validated @RequestBody UserSignUpRequestDto requestDto, BindingResult bindingResult) {
        if (bindingResult.hasFieldErrors()) throw new ValidationException(bindingResult);
        userService.signUp(requestDto);

        return new BaseResponse<>(new Object());
    }

    @PostMapping("/login")
    public BaseResponse<UserLoginResponseDto> login(@Validated @RequestBody UserLoginRequestDto requestDto, BindingResult bindingResult) {
        if(bindingResult.hasErrors()) throw new ValidationException(bindingResult);

        UserLoginResponseDto response = userService.login(requestDto);

        return new BaseResponse<>(response);
    }

}
