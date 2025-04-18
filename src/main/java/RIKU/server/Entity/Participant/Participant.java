package RIKU.server.Entity.Participant;

import RIKU.server.Entity.Base.BaseEntity;
import RIKU.server.Entity.Board.Post.Post;
import RIKU.server.Entity.User.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "participant")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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

    @Column(name = "participant_group")
    private String group;

    @Column(name = "participant_status")
    @Enumerated(EnumType.STRING)
    private ParticipantStatus participantStatus;

    private Participant(Post post, User user, String group, ParticipantStatus status) {
        this.post = post;
        this.user = user;
        this.group = group;
        this.participantStatus = status;
    }

    public static Participant createWithGroup(Post post, User user, String group) {
        return new Participant(post, user, group, ParticipantStatus.PENDING);
    }

    public static Participant create(Post post, User user) {
        return new Participant(post, user, null, ParticipantStatus.PENDING);
    }

    // 참여 의사 후 출석 코드 입력 시 상태 ATTENDED로 변경
    public void attend() {
        if (this.participantStatus == ParticipantStatus.ATTENDED) {
            throw new IllegalStateException("이미 출석 처리된 유저입니다.");
        }
        this.participantStatus = ParticipantStatus.ATTENDED;
    }

    // 참여 의사 후 출석 코드 미 입력 시 상태 ABSENT로 변경
    public void absent() {
        if (this.participantStatus == ParticipantStatus.ABSENT) {
            throw new IllegalStateException("이미 결석 처리된 유저입니다.");
        }
        this.participantStatus = ParticipantStatus.ABSENT;
    }

    public void updateParticipantStatus(ParticipantStatus status) {
        this.participantStatus = status;
    }
}
