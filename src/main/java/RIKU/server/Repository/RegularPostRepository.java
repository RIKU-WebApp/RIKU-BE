package RIKU.server.Repository;

import RIKU.server.Entity.Board.Post.Post;
import RIKU.server.Entity.Board.Post.RegularPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RegularPostRepository extends JpaRepository<RegularPost, Long> {

    Optional<RegularPost> findByPost(Post post);

    void deleteByPost(Post post);
}
