package RIKU.server.Entity.User;

import RIKU.server.Entity.BaseEntity;
import jakarta.persistence.*;
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
    private String loginId;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, unique = true)
    private String phone;

    @Column(nullable = false)
    private String major; // 학과

    @Column(name = "student_id", nullable = false, unique = true)
    private String studentId; // 학번

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "user_role")
    @Enumerated(EnumType.STRING)
    private UserRole userRole = UserRole.INACTIVE;

    public User(String loginId, String password, String name, String major, String studentId, String phone) {
        this.loginId = loginId;
        this.password = password;
        this.name = name;
        this.major = major;
        this.studentId = studentId;
        this.phone = phone;
    }
}
