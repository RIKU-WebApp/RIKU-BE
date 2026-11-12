package RIKU.server.domains.run.recruitment;
import org.springframework.data.jpa.repository.*;
import java.util.*;
import org.springframework.data.domain.Pageable;
public interface RunEnrollmentRepository extends JpaRepository<RunEnrollment, Long> {
    @Query("select e.user.id from RunEnrollment e where e.postId = ?1 and e.user.id in ?2")
    List<Long> findQueuedMemberIds(Long postId, List<Long> members);

    @EntityGraph(attributePaths = "user")
    @Query("select e from RunEnrollment e where e.postId = ?1 and (?2 is null or e.state = ?2) " +
            "and (?3 is null or e.id > ?3) order by e.id")
    List<RunEnrollment> nextEntries(Long postId, RunEnrollment.State state, Long after, Pageable page);

    interface Candidate {
        Long getId();
        String getGroupName();
    }

    @Query("select e.id as id, e.group as groupName from RunEnrollment e where e.postId = ?1 " +
            "and (?2 is null or e.state = ?2) and (?3 is null or e.id > ?3) order by e.id")
    List<Candidate> candidates(Long postId, RunEnrollment.State state, Long after, Pageable page);

    @EntityGraph(attributePaths = "user")
    List<RunEnrollment> findByIdInOrderByIdAsc(List<Long> ids);

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

    // Java treats case, accents and trailing spaces as significant in a group name.
    @Query(value = """
            select group_name as name, count(*) as used from run_enrollment
            where post_id = ?1 and (?2 is null or state = ?2)
            group by group_name, cast(group_name as binary) order by min(id)
            """, nativeQuery = true)
    List<GroupOccupancy> countOccupiedGroups(Long postId, String state);

    long countByPostIdAndState(Long postId, RunEnrollment.State state);
    @EntityGraph(attributePaths = "user")
    Optional<RunEnrollment> findFirstByPostIdAndStateOrderByIdAsc(Long postId, RunEnrollment.State state);

    Optional<RunEnrollment> findByPostIdAndUserId(Long postId, Long userId);
    void deleteByPostId(Long postId);
}
