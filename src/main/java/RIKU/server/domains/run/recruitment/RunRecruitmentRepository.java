package RIKU.server.domains.run.recruitment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface RunRecruitmentRepository extends JpaRepository<RunRecruitment, Long> {
    @Query("select distinct r from RunRecruitment r left join fetch r.groups where r.postId in :postIds")
    List<RunRecruitment> findWithGroupsByPostIds(@Param("postIds") List<Long> postIds);
}
