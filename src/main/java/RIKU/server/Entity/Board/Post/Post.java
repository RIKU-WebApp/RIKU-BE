package RIKU.server.Entity.Board.Post;

import RIKU.server.Entity.Base.BaseEntity;
import RIKU.server.Entity.Board.PostStatus;
import RIKU.server.Entity.User.User;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Entity
@Table(name = "post")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User postCreator; // 게시글 생성자

    private String title; // 게시글 제목

    private String location; // 집합 장소

    private LocalDateTime date; // 집합 날짜 및 시간

    @Lob
    private String content; // 게시글 내용

    @Column(name = "post_image_url")
    @Nullable
    private String postImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "post_status")
    private PostStatus postStatus;

    private Post (User postCreator, String title, String location, LocalDateTime date, String content, String postImageUrl, PostStatus postStatus) {
        this.postCreator = postCreator;
        this.title = title;
        this.location = location;
        this.date = date;
        this.content = content;
        this.postImageUrl = postImageUrl;
        this.postStatus = postStatus;
    }

    public static Post create(User postCreator, String title, String location, LocalDateTime date, String content, String postImageUrl) {
        return new Post(postCreator, title, location, date, content, postImageUrl, PostStatus.NOW);
    }

    public void updatePost(String title, String location, LocalDateTime date, String content, String postImageUrl) {
        this.title = title;
        this.location = location;
        this.date = date;
        this.content = content;
        this.postImageUrl = postImageUrl;
    }

    public void updateStatus(PostStatus status) { this.postStatus = status; }
}
