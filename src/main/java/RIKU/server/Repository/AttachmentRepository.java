package RIKU.server.Repository;

import RIKU.server.Entity.Board.Attachment;
import RIKU.server.Entity.Board.Post.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AttachmentRepository extends JpaRepository<Attachment, Long> {
    List<Attachment> findByPost(Post post);

    void deleteByPost(Post post);
}
