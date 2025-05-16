package RIKU.server.Repository;

import RIKU.server.Entity.Board.Post.Post;
import RIKU.server.Entity.Board.Post.TrainingPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TrainingPostRepository extends JpaRepository<TrainingPost, Long> {

    Optional<TrainingPost> findByPost(Post post);

    void deleteByPost(Post post);
}
