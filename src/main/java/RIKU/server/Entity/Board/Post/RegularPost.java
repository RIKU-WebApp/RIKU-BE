package RIKU.server.Entity.Board.Post;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "regular_post")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RegularPost {

    @Id
    @Column(name = "regular_post_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId         // PostJpaEntity의 PK를 공유
    @JoinColumn(name = "post_id")
    private Post post;

    @Column(name = "attendance_code")
    private String attendanceCode;

    private RegularPost (Post post, String attendanceCode) {
        this.post = post;
        this.attendanceCode = attendanceCode;
    }

    public static RegularPost create (Post post) {
        return new RegularPost(post, null);
    }
}
