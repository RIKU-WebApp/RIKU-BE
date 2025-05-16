package RIKU.server.Repository;

import RIKU.server.Entity.Board.Post.FlashPost;
import RIKU.server.Entity.Board.Post.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface FlashPostRepository extends JpaRepository<FlashPost, Long> {

    Optional<FlashPost> findByPost(Post post);

    void deleteByPost(Post post);

}
