package RIKU.server.Entity.Board.Post;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "event_post")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EventPost {

    @Id
    @Column(name = "event_post_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId         // PostJpaEntity의 PK를 공유
    @JoinColumn(name = "post_id")
    private Post post;

    @Column(name = "event_type")
    private String eventType;

    private EventPost (Post post, String eventType) {
        this.post = post;
        this.eventType = eventType;
    }

    public static EventPost create (Post post, String eventType) {
        return new EventPost(post, eventType);
    }

    public void updateEventType(String eventType) { this.eventType = eventType; }
}
