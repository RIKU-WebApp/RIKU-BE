package RIKU.server.Repository;

import RIKU.server.Entity.Board.FlashPost;
import RIKU.server.Entity.Board.Post;
import RIKU.server.Entity.Board.RegularPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    // 번개런 게시글 조회
    @Query("SELECT p FROM FlashPost p")
    List<FlashPost> findAllFlashPosts();

    // 정규런 게시글 조회
    @Query("SELECT p FROM RegularPost p")
    List<RegularPost> findAllRegularPosts();

    // 해당 날짜 게시글 조회
    List<Post> findByDateBetween(LocalDateTime startDate, LocalDateTime endDate);

}
