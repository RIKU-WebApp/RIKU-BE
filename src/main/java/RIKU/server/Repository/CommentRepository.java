package RIKU.server.Repository;

import RIKU.server.Entity.Board.Comment;
import RIKU.server.Entity.Board.Post.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByPost(Post post);

    List<Comment> findByTargetId(Long targetId);

    void deleteByPost(Post post);
}
