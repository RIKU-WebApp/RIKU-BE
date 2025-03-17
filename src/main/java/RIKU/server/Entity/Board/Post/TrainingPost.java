package RIKU.server.Entity.Board.Post;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "training_post")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TrainingPost {

    @Id
    @Column(name = "training_post_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId         // PostJpaEntity의 PK를 공유
    @JoinColumn(name = "post_id")
    private Post post;

    @Column(name = "attendance_code")
    private String attendanceCode;

    @Column(name = "training_type")
    private String trainingType;

    private TrainingPost (Post post, String attendanceCode, String trainingType) {
        this.post = post;
        this.attendanceCode = attendanceCode;
        this.trainingType = trainingType;
    }

    public static TrainingPost create (Post post, String trainingType) {
        return new TrainingPost(post, null, trainingType);
    }
}
