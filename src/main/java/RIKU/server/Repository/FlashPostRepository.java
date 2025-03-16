package RIKU.server.Repository;

import RIKU.server.Entity.Board.Post.FlashPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FlashPostRepository extends JpaRepository<FlashPost, Long> {


}
