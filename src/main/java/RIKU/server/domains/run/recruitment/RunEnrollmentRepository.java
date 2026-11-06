package RIKU.server.domains.run.recruitment;
import org.springframework.data.jpa.repository.*;
import java.util.*;
public interface RunEnrollmentRepository extends JpaRepository<RunEnrollment, Long> {
    interface RosterRow {
        Long getUserId();
        String getName();
        RunEnrollment.Role getRole();
        RunEnrollment.State getState();
        String getGroupName();
    }

    @Query("select e.user.id as userId, e.user.name as name, e.role as role, e.state as state, e.group as groupName " +
            "from RunEnrollment e where e.postId = ?1 order by e.id")
    List<RosterRow> findRoster(Long postId);

    @Query("select e.group from RunEnrollment e where e.postId = ?1 and e.state = ?2")
    List<String> findOccupiedGroups(Long postId, RunEnrollment.State state);

    long countByPostIdAndState(Long postId, RunEnrollment.State state);
    @EntityGraph(attributePaths = "user")
    Optional<RunEnrollment> findFirstByPostIdAndStateOrderByIdAsc(Long postId, RunEnrollment.State state);

    @EntityGraph(attributePaths = "user")
    List<RunEnrollment> findByPostIdOrderByIdAsc(Long postId);
    Optional<RunEnrollment> findByPostIdAndUserId(Long postId, Long userId);
    void deleteByPostId(Long postId);
}
