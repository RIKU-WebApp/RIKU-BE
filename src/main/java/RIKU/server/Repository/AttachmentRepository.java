package RIKU.server.Repository;

import RIKU.server.Entity.Board.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AttachmentRepository extends JpaRepository<Attachment, Long
        > {
}
