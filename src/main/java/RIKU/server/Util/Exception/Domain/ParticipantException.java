package RIKU.server.Util.Exception.Domain;

import RIKU.server.Util.BaseResponseStatus;
import lombok.Getter;

@Getter
public class ParticipantException extends CustomException {
    public ParticipantException(BaseResponseStatus status) { super(status); }
}
