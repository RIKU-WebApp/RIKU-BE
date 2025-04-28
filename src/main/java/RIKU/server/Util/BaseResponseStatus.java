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
    USER_NOT_A_PACER(false, 2019, "해당 유저는 페이서가 아닙니다."),
    INVALID_FIELD(false, 2020, "요청 값이 잘못되었습니다."),
    EMPTY_REQUEST_PARAMETER(false, 2021, "Request Parameter가 존재하지 않습니다."),
    METHOD_ARGUMENT_TYPE_MISMATCH(false, 2022, "Request Parameter나 Path Variable의 유형이 불일치합니다."),
    PROFILE_IMAGE_UPLOAD_FAILED(false, 2023, "프로필 이미지 업로드에 실패하였습니다."),
    ALREADY_ATTENDED_TODAY(false, 2024, "이미 오늘 출석했습니다."),

    // PostException
    POST_NOT_FOUND(false, 3001, "존재하지 않는 게시글입니다."),
    UNAUTHORIZED_POST_ACCESS(false, 3002, "해당 게시글에 접근할 권한이 없습니다."),
    POST_CREATION_FAILED(false, 3003, "게시글 생성에 실패하였습니다."),
    POST_IMAGE_UPLOAD_FAILED(false, 3004, "게시글 이미지 업로드에 실패하였습니다."),
    ATTACHMENT_UPLOAD_FAILED(false, 3005, "게시글 첨부파일 업로드에 실패하였습니다."),
    INVALID_DATE_AND_TIME(false, 3006, "유효하지 않은 집합 날짜 및 시간입니다."),
    COMMENT_NOT_FOUND(false, 3007, "존재하지 않는 댓글입니다."),
    INVALID_COMMENT_FOR_POST(false, 3008, "게시글에 속한 댓글이 아닙니다."),
    ALREADY_ASSIGNED_PACER(false, 3009, "이미 등록된 페이서입니다."),
    INVALID_POST_TYPE(false, 3010, "해당 게시글 유형과 일치하지 않습니다."),
    INVALID_RUN_TYPE(false, 3010, "유효하지 않은 runType입니다."),
    UNAUTHORIZED_POST_TYPE(false, 3011, "해당 게시글 유형은 권한이 없습니다."),
    FLASH_POST_NOT_FOUND(false, 3012, "존재하지 않는 번개런 게시글입니다."),
    REGULAR_POST_NOT_FOUND(false, 3013, "존재하지 않는 정규런 게시글입니다."),
    TRAINING_POST_NOT_FOUND(false, 3014, "존재하지 않는 훈련 게시글입니다."),
    EVENT_POST_NOT_FOUND(false, 3015, "존재하지 않는 행사 게시글입니다."),
    PACER_NOT_FOUND(false, 3016, "존재하지 않는 페이서입니다."),
    CREATOR_NOT_IN_PACER_LIST(false, 3017, "생성자는 페이서 리스트에 포함되어야 합니다."),
    DUPLICATED_PACER(false, 3018, "페이서가 중복 지정되었습니다."),
    INVALID_POST_STATUS(false, 3019, "해당 게시글은 취소되거나 종료되었습니다."),
    INVALID_PACER_COUNT(false, 3020, "수정할 페이서 수는 기존과 동일해야 합니다."),
    DUPLICATE_POST(false, 3021, "동일한 제목과 날짜로 이미 등록된 게시글이 존재합니다."),

    // ParticipantException
    ALREADY_PARTICIPATED(false, 4001, "이미 참여한 유저입니다."),
    INVALID_ATTENDANCE_CODE(false, 4002, "출석 코드가 일치하지 않습니다."),
    NOT_PARTICIPATED(false, 4003, "참여자가 아닙니다."),
    ALREADY_ATTENDED(false, 4004, "이미 출석했습니다."),
    ATTENDANCE_CODE_NOT_FOUND(false, 4005, "출석 코드가 존재하지 않습니다."),
    ALREADY_CLOSED_POST(false, 4006, "이미 종료 처리된 게시글입니다."),
    INVALID_ATTENDANCE_TIME(false, 4007, "출석 시간이 유효하지 않습니다."),
    GROUP_REQUIRED(false, 4008, "정규런/훈련 참여 시 그룹 정보는 필수입니다."),
    INVALID_PARTICIPANT_GROUP(false, 4009, "그룹이 지정되지 않은 참여자가 존재합니다."),
    ATTENDANCE_CODE_NOT_YET_CREATED(false, 4010, "출석 코드가 아직 생성되지 않았습니다."),
    PACER_CANNOT_PARTICIPATE(false, 4011, "페이서는 직접 참여하거나 출석할 수 없습니다."),

    /**
     * 9000 : MultipartFile 오류
     */

    IS_NOT_IMAGE_FILE(false, 9000, "지원되는 이미지 파일의 형식이 아닙니다."),
    MULTIPARTFILE_CONVERT_FAIL_IN_MEMORY(false, 9001,"multipartFile memory 변환 과정에서 문제가 생겼습니다."),
    FILE_SIZE_EXCEEDED(false, 9002, "파일 크기가 10MB를 초과했습니다."),
    REQUEST_SIZE_EXCEEDED(false, 9003, "파일 요청 전체 크기가 20MB를 초과했습니다.")

    ;

    private final boolean isSuccess;
    private final int responseCode;
    private final String responseMessage;

}
