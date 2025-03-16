package RIKU.server.Repository;

import RIKU.server.Entity.Base.BaseStatus;
import RIKU.server.Entity.Board.Post.Post;
import RIKU.server.Entity.Board.Post.PostType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    List<Post> findByStatusAndPostType(BaseStatus baseStatus, PostType postType);

    // 해당 날짜 게시글 조회
    List<Post> findByDateBetween(LocalDateTime startDate, LocalDateTime endDate);

    // 게시글 오름차순 정렬 조회
    List<Post> findByDateAfterOrderByDateAsc(LocalDateTime date);

}
