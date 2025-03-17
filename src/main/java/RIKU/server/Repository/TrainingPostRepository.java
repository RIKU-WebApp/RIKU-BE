package RIKU.server.Repository;

import RIKU.server.Entity.Board.Post.TrainingPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TrainingPostRepository extends JpaRepository<TrainingPost, Long> {
}
