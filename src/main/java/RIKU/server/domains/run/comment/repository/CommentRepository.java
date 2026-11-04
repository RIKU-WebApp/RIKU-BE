package RIKU.server.domains.run.comment.repository;

import RIKU.server.domains.run.comment.entity.Comment;
import RIKU.server.domains.run.post.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByPost(Post post);

    @Query("select c from Comment c left join fetch c.user where c.post.id = :postId order by c.id")
    List<Comment> findWithUserByPostId(@Param("postId") Long postId);

    List<Comment> findByTargetId(Long targetId);

    void deleteByPost(Post post);
}
