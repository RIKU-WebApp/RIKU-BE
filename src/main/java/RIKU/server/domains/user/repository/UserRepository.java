package RIKU.server.domains.user.repository;

import RIKU.server.global.entity.BaseStatus;
import RIKU.server.domains.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import RIKU.server.domains.point.ranking.RankingMember;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    default Map<Long, User> findMembersByIds(List<Long> ids) {
        var members = new HashMap<Long, User>();
        for (int start = 0; start < ids.size(); start += 500) {
            findAllById(ids.subList(start, Math.min(start + 500, ids.size())))
                    .forEach(member -> members.put(member.getId(), member));
        }
        return members;
    }

    @Query("select new RIKU.server.domains.point.ranking.RankingMember(u.id, u.name, u.profileImageUrl, u.userRole) " +
            "from User u where u.status = :status and u.id not in :excludedIds")
    List<RankingMember> findRankingMembers(@Param("status") BaseStatus status, @Param("excludedIds") Set<Long> excludedIds);

    // 학번으로 조회
    Optional<User> findByStudentId(String studentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.studentId = :studentId")
    Optional<User> findForRenewal(@Param("studentId") String studentId);

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
