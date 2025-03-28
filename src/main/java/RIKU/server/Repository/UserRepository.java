package RIKU.server.Repository;

import RIKU.server.Entity.Base.BaseStatus;
import RIKU.server.Entity.User.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // 학번으로 조회
    Optional<User> findByStudentId(String studentId);

    // 상태로 조회
    List<User> findByStatus(BaseStatus status);

    // 학번 중복 확인
    boolean existsByStudentId(String studentId);

    // 페이서 조회
    List<User> findByIsPacer(Boolean isPacer);
}
