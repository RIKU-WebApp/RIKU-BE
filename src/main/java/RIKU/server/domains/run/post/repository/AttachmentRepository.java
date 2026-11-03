package RIKU.server.domains.run.post.repository;

import RIKU.server.domains.run.post.entity.Attachment;
import RIKU.server.domains.run.post.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AttachmentRepository extends JpaRepository<Attachment, Long> {
    List<Attachment> findByPost(Post post);

    void deleteByPost(Post post);
}
