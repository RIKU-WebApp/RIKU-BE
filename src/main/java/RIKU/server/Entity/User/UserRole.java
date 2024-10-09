package RIKU.server.Entity.User;

public enum UserRole {
    NEW_MEMBER, // 신입 부원
    MEMBER,     // 일반 부원 (번개런 생성 가능)
    ADMIN,      // 운영진 및 페이서 (모든 권한 부여)
    INACTIVE    // 비활성화 사용자
}
