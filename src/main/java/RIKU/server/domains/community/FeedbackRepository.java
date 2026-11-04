package RIKU.server.domains.community;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.*;
public interface FeedbackRepository extends JpaRepository<Feedback,Long> {
    Page<Feedback> findByUserIdOrderByIdDesc(Long id,Pageable page);
}
