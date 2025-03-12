package RIKU.server.Entity.Participant;

import RIKU.server.Entity.Base.BaseEntity;
import RIKU.server.Entity.Board.Post.Post;
import RIKU.server.Entity.User.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "participant")
@Getter
@NoArgsConstructor
public class Participant extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "participant_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;  // 참여한 사용자

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Post post;

    @Column(name = "participant_status")
    @Enumerated(EnumType.STRING)
    private ParticipantStatus status = ParticipantStatus.PENDING; // 기본값 출석 대기

    public Participant(Post post, User user) {
        this.post = post;
        this.user = user;
    }

    // 참여 의사 후 출석 코드 입력 시 상태 ATTENDED로 변경
    public void attend() {
        this.status = ParticipantStatus.ATTENDED;
    }

    // 참여 의사 후 출석 코드 미 입력 시 상태 ABSENT로 변경
    public void absent() {
        this.status = ParticipantStatus.ABSENT;
    }
}
