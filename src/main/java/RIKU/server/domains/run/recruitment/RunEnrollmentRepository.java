package RIKU.server.domains.run.recruitment;
import org.springframework.data.jpa.repository.*;
import java.util.*;
public interface RunEnrollmentRepository extends JpaRepository<RunEnrollment, Long> {
    long countByPostIdAndState(Long postId, RunEnrollment.State state);
    @EntityGraph(attributePaths = "user")
    Optional<RunEnrollment> findFirstByPostIdAndStateOrderByIdAsc(Long postId, RunEnrollment.State state);

    @EntityGraph(attributePaths = "user")
    List<RunEnrollment> findByPostIdOrderByIdAsc(Long postId);
    Optional<RunEnrollment> findByPostIdAndUserId(Long postId, Long userId);
    void deleteByPostId(Long postId);
}
