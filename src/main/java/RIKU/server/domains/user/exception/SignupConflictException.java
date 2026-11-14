package RIKU.server.domains.user.exception;

import RIKU.server.global.response.BaseResponseStatus;
import lombok.Getter;

@Getter
public class SignupConflictException extends UserException {
    private final String field;

    private SignupConflictException(BaseResponseStatus status, String field) {
        super(status);
        this.field = field;
    }

    public static SignupConflictException phone() {
        return new SignupConflictException(BaseResponseStatus.DUPLICATED_PHONE, "phone");
    }

    public static SignupConflictException studentId() {
        return new SignupConflictException(BaseResponseStatus.DUPLICATED_STUDENTID, "studentId");
    }
}
