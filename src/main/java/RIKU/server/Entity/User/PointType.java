package RIKU.server.Entity.User;

public enum PointType {
    ADD_REGULAR_JOIN,   // 정규런 참여(+10)
    ADD_FLASH_CREATE,   // 번개런 생성(+5)
    ADD_FLASH_JOIN,     // 번개런 참여(+5)
    ADD_TRAINING_JOIN,  // 훈련 참여(+7)
    ADD_ATTENDANCE,     // 마이페이지 출석(+1)
    REMOVE   // 차감
}
