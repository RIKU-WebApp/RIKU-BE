package RIKU.server.domains.run.post.repository;

import RIKU.server.global.entity.BaseStatus;
import RIKU.server.domains.run.post.entity.Post;
import RIKU.server.domains.run.post.entity.PostType;
import RIKU.server.domains.run.post.entity.PostStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    List<Post> findByStatusAndPostType(BaseStatus baseStatus, PostType postType);

    // 해당 날짜 게시글 조회
    List<Post> findByStatusAndDateBetween(BaseStatus status, LocalDateTime startDate, LocalDateTime endDate);

    // 러닝 유형별 가장 가까운 날짜의 게시글 1개 조회
    Optional<Post> findTopByStatusAndPostTypeAndPostStatusOrderByDateAsc(BaseStatus status, PostType postType, PostStatus postStatus);

    // 같은 제목, 같은 작성자, 같은 날짜가 존재하는 게시글 조회
    Optional<Post> findByPostCreatorIdAndTitleAndDate(Long creatorId, String title, LocalDateTime date);

}
