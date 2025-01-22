package RIKU.server.Entity.Board;

import RIKU.server.Entity.BaseEntity;
import RIKU.server.Entity.Participant.Participant;
import RIKU.server.Entity.User.User;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static jakarta.persistence.FetchType.LAZY;

@Entity
@Table(name = "Posts")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "post_type")
@Getter
@NoArgsConstructor
@Slf4j
public abstract class Post extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_id")
    private Long id;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "user_id")
    private User createdBy; // 게시글 생성자

    private String title; // 게시글 제목

    private String location; // 집합 장소

    private LocalDateTime date; // 집합 날짜 및 시간

    @Column(columnDefinition = "Text")
    private String content; // 게시글 내용

    @Column(name = "post_image_url")
    @Nullable
    private String postImageUrl;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(name = "post_status")
    private PostStatus postStatus = PostStatus.NOW; // 모집 상태(모집중)

    @Column(name = "attendance_code")
    private String attendanceCode;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Participant> Participants = new ArrayList<>();

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> comments;

    public Post (User createdBy, String title, String location, LocalDateTime date, String content, String postImageUrl) {
        this.createdBy = createdBy;
        this.title = title;
        this.location = location;
        this.date = date;
        this.content = content;
        this.postImageUrl = postImageUrl;
    }

    public void updatePost(String title, String location, LocalDateTime date, String content, String postImageUrl) {
        log.debug("Updating post: title = {}, location = {}, date = {}, content = {}, postImageUrl = {}", title, location, date, content, postImageUrl);
        this.title = title;
        this.location = location;
        this.date = date;
        this.content = content;
        this.postImageUrl = postImageUrl;
    }


    // 출석 코드를 생성하는 메서드
    public String createdAttendanceCode() {
        this.attendanceCode = String.valueOf((int) (Math.random() * 900) + 100);
        return attendanceCode;
    }

    public void updateStatus(PostStatus status) { this.postStatus = status; }

}
