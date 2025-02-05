package RIKU.server.Repository;

import RIKU.server.Entity.User.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByStudentId(String studentId);

    boolean existsByStudentId(String studentId);

    // 포인트 기준 Top 10 유저 조회
    List<User> findTop10ByOrderByTotalPointsDescNameAsc();

    // 전체 유저를 포인트 내림차순 정렬하여 조회
    List<User> findAllByOrderByTotalPointsDescNameAsc();
}
