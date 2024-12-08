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

    // JWT 예외 - Filter에서 처리
    ACCESS_DENIED(false, 2001, "권한이 없는 유저의 접근입니다."),
    EMPTY_AUTHORIZATION_HEADER(false, 2002, "Authorization 헤더가 존재하지 않습니다."),
    EXPIRED_ACCESS_TOKEN(false, 2003, "이미 만료된 Access 토큰입니다."),
    UNSUPPORTED_TOKEN_TYPE(false, 2004, "지원되지 않는 토큰 형식입니다."),
    MALFORMED_TOKEN_TYPE(false, 2005, "인증 토큰이 올바르게 구성되지 않았습니다."),
    INVALID_SIGNATURE_JWT(false, 2006, "인증 시그니처가 올바르지 않습니다"),
    INVALID_TOKEN_TYPE(false, 2007, "잘못된 토큰입니다."),

    // Refresh Token 예외 - Exception Handler에서 처리
    EXPIRED_REFRESH_TOKEN(false, 2008, "Refresh 토큰이 만료되어 재로그인이 필요합니다."),
    INVALID_REFRESH_TOKEN(false, 2009, "잘못된 Refresh 토큰입니다."),

    // IdToken 예외
    EXPIRED_ID_TOKEN(false, 2010, "이미 만료된 ID 토큰입니다."),

    // UserException
    INVALID_USER_SIGNUP(false, 2011, "회원가입 요청에서 잘못된 값이 존재합니다."),
    DUPLICATED_STUDENTID(false, 2012, "중복된 아이디입니다."),
    USER_NOT_FOUND(false, 2013, "존재하지 않는 회원입니다."),
    INVALID_PASSWORD(false, 2014, "비밀번호가 일치하지 않습니다."),
    INVALID_USER_LOGIN(false, 2015, "로그인 요청에서 잘못된 값이 존재합니다."),
    INVALID_USER_ROLE(false, 2016, "존재하지 않는 역할입니다."),
    UNAUTHORIZED_USER(false, 2017, "권한이 없는 회원입니다."),
    ROLE_ALREADY_ASSIGNED(false, 2018, "이미 해당 역할이 할당되어 있습니다."),

    INVALID_FIELD(false, 2020, "요청 값이 잘못되었습니다."),
    EMPTY_REQUEST_PARAMETER(false, 2021, "Request Parameter가 존재하지 않습니다."),
    METHOD_ARGUMENT_TYPE_MISMATCH(false, 2022, "Request Parameter나 Path Variable의 유형이 불일치합니다."),

    // PostException
    POST_NOT_FOUND(false, 3001, "존재하지 않는 게시글입니다."),
    UNAUTHORIZED_POST_ACCESS(false, 3002, "해당 게시글에 접근할 권한이 없습니다."),
    POST_CREATION_FAILED(false, 3003, "게시글 생성에 실패하였습니다."),
    POST_IMAGE_UPLOAD_FAILED(false, 3004, "게시글 이미지 업로드에 실패하였습니다."),
    INVALID_RUN_TYPE(false, 3005, "존재하지 않는 게시글 타입입니다."),

    // ParticipantException
    ALREADY_PARTICIPATED(false, 4001, "이미 참여한 유저입니다."),
    INVALID_ATTENDANCE_CODE(false, 4002, "출석 코드가 일치하지 않습니다."),
    NOT_PARTICIPATED(false, 4003, "참여자가 아닙니다."),
    ALREADY_ATTENDED(false, 4004, "이미 출석했습니다."),

    /**
     * 9000 : MultipartFile 오류
     */

    IS_NOT_IMAGE_FILE(false, 9000, "지원되는 이미지 파일의 형식이 아닙니다."),
    MULTIPARTFILE_CONVERT_FAIL_IN_MEMORY(false, 9001,"multipartFile memory 변환 과정에서 문제가 생겼습니다.")



    ;

    private final boolean isSuccess;
    private final int responseCode;
    private final String responseMessage;

}
