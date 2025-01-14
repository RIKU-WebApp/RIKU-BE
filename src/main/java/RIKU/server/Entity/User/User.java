package RIKU.server.Entity.User;

import RIKU.server.Entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

    @Column(name = "student_id", nullable = false, unique = true)
    private String studentId; // 학번

    @Setter
    @Column(nullable = false)
    private String password;

    @Setter
    @Column(unique = true)
    private String phone;

    @Column(nullable = false)
    private String college; // 단과대학

    @Column(nullable = false)
    private String major; // 학과

    @Setter
    @Column(name = "profile_image_url")
    private String profileImageUrl;

    @Setter
    @Column(name = "user_role")
    @Enumerated(EnumType.STRING)
    private UserRole userRole = UserRole.MEMBER;

    @Builder
    public User(String studentId, String password, String name, String college, String major, String phone) {
        this.studentId = studentId;
        this.password = password;
        this.name = name;
        this.college = college;
        this.major = major;
        this.phone = phone;
    }

}
