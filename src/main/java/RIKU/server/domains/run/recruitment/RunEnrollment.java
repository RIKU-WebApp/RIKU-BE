package RIKU.server.domains.run.recruitment;

import RIKU.server.domains.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity @Getter @NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"post_id", "user_id"}))
public class RunEnrollment {
    public enum Role { RUNNER, PACER }
    public enum State { CONFIRMED, WAITLISTED }
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "post_id", nullable = false) private Long postId;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id", nullable = false) private User user;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Role role;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private State state;
    @Column(name = "group_name", nullable = false) private String group;
    @Column(nullable = false) private Instant registeredAt;
    public RunEnrollment(Long postId, User user, Role role, String group, State state) {
        this.postId=postId; this.user=user; this.role=role; this.group=group; this.state=state;
        this.registeredAt=Instant.now();
    }
    public void confirm() {
        if (role == Role.PACER && !Boolean.TRUE.equals(user.getIsPacer())) role = Role.RUNNER;
        state = State.CONFIRMED;
    }
    public void changeGroup(String group) { this.group=group; }
}
