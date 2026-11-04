package RIKU.server.domains.user.repository;

import RIKU.server.global.entity.BaseStatus;
import RIKU.server.domains.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import RIKU.server.domains.point.ranking.RankingMember;
import java.util.Set;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    @Query("select new RIKU.server.domains.point.ranking.RankingMember(u.id, u.name, u.profileImageUrl) " +
            "from User u where u.status = :status and u.id not in :excludedIds")
    List<RankingMember> findRankingMembers(@Param("status") BaseStatus status, @Param("excludedIds") Set<Long> excludedIds);

    // 학번으로 조회
    Optional<User> findByStudentId(String studentId);

    List<User> findByStudentIdIn(List<String> studentIds);

    // 상태로 조회
    List<User> findByStatus(BaseStatus status);

    // 학번 중복 확인
    boolean existsByStudentId(String studentId);

    // 페이서 조회
    List<User> findByIsPacer(Boolean isPacer);

    // 이름으로 조회
    List<User> findByNameContainingIgnoreCaseOrderByNameAscIdAsc(String name);
}
