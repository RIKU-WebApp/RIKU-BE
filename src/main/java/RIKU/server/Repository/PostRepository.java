package RIKU.server.Repository;

import RIKU.server.Entity.Base.BaseStatus;
import RIKU.server.Entity.Board.Post.Post;
import RIKU.server.Entity.Board.Post.PostType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    List<Post> findByStatusAndPostType(BaseStatus baseStatus, PostType postType);

    // 해당 날짜 게시글 조회
    List<Post> findByDateBetween(LocalDateTime startDate, LocalDateTime endDate);

    // 러닝 유형별 가장 가까운 날짜의 게시글 1개 조회
    Optional<Post> findTopByStatusAndPostTypeAndDateAfterOrderByDateAsc(BaseStatus status, PostType postType, LocalDateTime now);

}
