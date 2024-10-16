package RIKU.server.Entity.User;

import RIKU.server.Entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "Users")
@Getter
@NoArgsConstructor
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "login_id", nullable = false, unique = true)
    private String loginId; // 학번

    @Column(nullable = false)
    private String password;

    @Column(unique = true)
    private String phone;

    private String college; // 단과대학

    @Column(nullable = false)
    private String major; // 학과

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "user_role")
    @Enumerated(EnumType.STRING)
    private UserRole userRole = UserRole.INACTIVE;

    @Builder
    public User(String loginId, String password, String name, String college, String major, String phone) {
        this.loginId = loginId;
        this.password = password;
        this.name = name;
        this.college = college;
        this.major = major;
        this.phone = phone;
    }
}
