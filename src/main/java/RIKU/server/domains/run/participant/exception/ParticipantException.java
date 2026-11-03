package RIKU.server.domains.run.participant.exception;

import RIKU.server.global.exception.CustomException;

import RIKU.server.global.response.BaseResponseStatus;
import lombok.Getter;

@Getter
public class ParticipantException extends CustomException {
    public ParticipantException(BaseResponseStatus status) { super(status); }
}
