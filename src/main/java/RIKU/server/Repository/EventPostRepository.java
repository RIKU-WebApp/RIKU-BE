package RIKU.server.Repository;

import RIKU.server.Entity.Board.Post.EventPost;
import RIKU.server.Entity.Board.Post.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EventPostRepository extends JpaRepository<EventPost, Long> {
    Optional<EventPost> findByPost(Post post);
}
