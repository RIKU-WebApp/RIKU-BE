package RIKU.server.domains.run.post.repository;

import RIKU.server.domains.run.post.entity.Post;
import RIKU.server.domains.run.post.entity.TrainingPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TrainingPostRepository extends JpaRepository<TrainingPost, Long> {

    Optional<TrainingPost> findByPost(Post post);

    void deleteByPost(Post post);
}
