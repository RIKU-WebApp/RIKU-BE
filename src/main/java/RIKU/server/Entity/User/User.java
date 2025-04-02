package RIKU.server.Entity.User;

import RIKU.server.Entity.Base.BaseEntity;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    private String name;

    @Column(name = "student_id", unique = true)
    private String studentId; // 학번

    private String password;

    @Column(unique = true)
    @Nullable
    private String phone;

    private String college; // 단과대학

    private String major; // 학과

    @Column(name = "profile_image_url")
    @Nullable
    private String profileImageUrl;

    @Column(name = "user_role")
    @Enumerated(EnumType.STRING)
    private UserRole userRole;

    @Column(name = "is_pacer")
    private Boolean isPacer;

    private User(String studentId, String password, String name, String college, String major, String phone, UserRole userRole, Boolean isPacer) {
        this.studentId = studentId;
        this.password = password;
        this.name = name;
        this.college = college;
        this.major = major;
        this.phone = phone;
        this.userRole = userRole;
        this.isPacer = isPacer;
    }

    public static User create(String studentId, String password, String name, String college, String major, String phone) {
        return new User(studentId, password, name, college, major, phone, UserRole.MEMBER, Boolean.FALSE);
    }

    public void updateProfile(String phone, String password, String profileImageUrl) {
        this.phone = phone;
        this.password = password;
        this.profileImageUrl = profileImageUrl;
    }

    public void updatePacer(Boolean isPacer) { this.isPacer = isPacer; }

    public void updateUserRole(UserRole userRole) {
        this.userRole = userRole;
    }
}
