package RIKU.server.Repository;

import RIKU.server.Entity.Board.Post.EventPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EventPostRepository extends JpaRepository<EventPost, Long> {
}
