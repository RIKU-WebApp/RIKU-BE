package RIKU.server.Repository;

import RIKU.server.Entity.Board.Comment;
import RIKU.server.Entity.Board.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    // 특정 게시글의 댓글 조회
    List<Comment> findByPost(Post post);

    // 대댓글 조회
    List<Comment> findByTargetId(Long targetId);
}
