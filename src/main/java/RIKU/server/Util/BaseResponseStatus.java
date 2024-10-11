package RIKU.server.Util;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum BaseResponseStatus {

    /**
     * 1000: 요청 성공
     */
    SUCCESS(true, 1000, "요청에 성공하였습니다."),

    /**
     * 2000: Request 오류 (BAD_REQUEST)
     */
    BAD_REQUEST(false, 2000, "잘못된 요청입니다."),

    // UserException
    INVALID_USER_SIGNUP(false, 2001, "회원가입 요청에서 잘못된 값이 존재합니다."),
    DUPLICATED_LOGINID(false, 2002, "중복된 로그인 아이디입니다."),
    USER_NOT_FOUND(false, 2003, "존재하지 않는 회원입니다."),
    INVALID_PASSWORD(false, 2004, "비밀번호가 일치하지 않습니다."),
    INVALID_USER_LOGIN(false, 2005, "로그인 요청에서 잘못된 값이 존재합니다."),
    ;

    private final boolean isSuccess;
    private final int responseCode;
    private final String responseMessage;

}
