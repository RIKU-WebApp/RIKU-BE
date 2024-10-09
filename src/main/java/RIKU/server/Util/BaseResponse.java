package RIKU.server.Util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;

import static RIKU.server.Util.BaseResponseStatus.*;

@Getter
@JsonPropertyOrder({"isSuccess", "responseCode", "responseMessage", "result"})
public class BaseResponse<T> {

    @JsonProperty("isSuccess")
    private final Boolean isSuccess;

    private final int responseCode;
    private final String responseMessage;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private final T result;

    // Request Success, Request 성공시 무조건 result 존재
    public BaseResponse(T result){
        this.isSuccess = SUCCESS.isSuccess();
        this.responseMessage = SUCCESS.getResponseMessage();
        this.responseCode = SUCCESS.getResponseCode();
        this.result = result;
    }

    // Common Exception, 보내는 데이터 없이 메시지만 출력하면 되는 에러 처리
    public BaseResponse(BaseResponseStatus status) {
        this.isSuccess = status.isSuccess();
        this.responseMessage = status.getResponseMessage();
        this.responseCode = status.getResponseCode();
        this.result = null;
    }

    // validated Exception, 검증 예외 처리,
    public BaseResponse(BaseResponseStatus status, T result) {
        this.isSuccess = status.isSuccess();
        this.responseMessage = status.getResponseMessage();
        this.responseCode = status.getResponseCode();
        this.result = result;
    }
}
