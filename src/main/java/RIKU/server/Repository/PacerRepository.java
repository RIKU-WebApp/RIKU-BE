package RIKU.server.Repository;

import RIKU.server.Entity.Board.Pacer;
import RIKU.server.Entity.Board.Post.Post;
import RIKU.server.Entity.User.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PacerRepository extends JpaRepository<Pacer, Long> {

    boolean existsByUserAndPost(User user, Post post);
}
