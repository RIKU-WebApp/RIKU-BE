package RIKU.server.Entity.User;

public enum PointType {
    ADD_REGULAR_JOIN,   // 정규런 참여(+10)
    ADD_FLASH_CREATE,   // 번개런 생성(+7)
    ADD_FLASH_JOIN,     // 번개런 참여(+5)
    ADD_TRAINING_JOIN,  // 훈련 참여(+8)
    ADD_EVENT_JOIN,     // 행사 참여(+8)
    ADD_ATTENDANCE,     // 오늘의 출석(+1)
    REMOVE   // 차감
}
