package RIKU.server.Entity.User;

import RIKU.server.Entity.BaseEntity;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Entity
@Table(name = "Users")
@Getter
@NoArgsConstructor
@Slf4j
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

    public void updateProfile(String phone, String password, String profileImageUrl) {
        log.debug("Updating user profile: phone = {}, password = {}, profileImageUrl = {}", phone, password, profileImageUrl);
        this.phone = phone;
        this.password = password;
        this.profileImageUrl = profileImageUrl;
    }

}
