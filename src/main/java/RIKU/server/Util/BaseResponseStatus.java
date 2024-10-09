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
    DUPLICATED_LOGINID(false, 2001, "중복된 로그인 아이디입니다.")
    ;

    private final boolean isSuccess;
    private final int responseCode;
    private final String responseMessage;

}
