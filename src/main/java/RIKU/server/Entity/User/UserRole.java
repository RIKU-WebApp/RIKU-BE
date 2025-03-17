package RIKU.server.Entity.User;

import lombok.Getter;

@Getter
public enum UserRole {
    NEW_MEMBER("NEW_MEMBER"),   // 신입 부원
    MEMBER("MEMBER"),     // 일반 부원 (번개런 생성 가능)
    ADMIN("ADMIN");     // 운영진 및 페이서 (모든 권한 부여)

    UserRole(String value) {
        this.value = value;
        this.role = PREFIX + value;
    }

    private final String PREFIX = "ROLE_";
    private final String value;
    private final String role;
}
