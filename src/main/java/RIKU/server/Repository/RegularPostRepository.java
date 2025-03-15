package RIKU.server.Repository;

import RIKU.server.Entity.Board.Post.RegularPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RegularPostRepository extends JpaRepository<RegularPost, Long> {


}
