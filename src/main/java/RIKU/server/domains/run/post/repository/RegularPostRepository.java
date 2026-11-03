package RIKU.server.domains.run.post.repository;

import RIKU.server.domains.run.post.entity.Post;
import RIKU.server.domains.run.post.entity.RegularPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RegularPostRepository extends JpaRepository<RegularPost, Long> {

    Optional<RegularPost> findByPost(Post post);

    void deleteByPost(Post post);
}
