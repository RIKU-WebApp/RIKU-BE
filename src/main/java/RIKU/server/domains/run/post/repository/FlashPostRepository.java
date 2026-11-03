package RIKU.server.domains.run.post.repository;

import RIKU.server.domains.run.post.entity.FlashPost;
import RIKU.server.domains.run.post.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface FlashPostRepository extends JpaRepository<FlashPost, Long> {

    Optional<FlashPost> findByPost(Post post);

    void deleteByPost(Post post);

}
