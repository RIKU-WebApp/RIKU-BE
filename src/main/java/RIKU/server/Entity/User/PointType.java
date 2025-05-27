package RIKU.server.Entity.User;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "포인트 타입")
public enum PointType {
    @Schema(description = "정규런 참여 (+10)")
    ADD_REGULAR_JOIN,

    @Schema(description = "번개런 생성 (+7)")
    ADD_FLASH_CREATE,

    @Schema(description = "번개런 참여 (+5)")
    ADD_FLASH_JOIN,

    @Schema(description = "훈련 참여 (+8)")
    ADD_TRAINING_JOIN,

    @Schema(description = "행사 참여 (+8)")
    ADD_EVENT_JOIN,

    @Schema(description = "출석 (+1)")
    ADD_ATTENDANCE,

    @Schema(description = "포인트 차감")
    REMOVE
}
