package RIKU.server.domains.run.comment.repository;

import RIKU.server.domains.run.comment.entity.Comment;
import RIKU.server.domains.run.post.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByPost(Post post);

    List<Comment> findByTargetId(Long targetId);

    void deleteByPost(Post post);
}
