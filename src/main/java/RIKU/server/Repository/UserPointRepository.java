package RIKU.server.Repository;

import RIKU.server.Entity.User.PointType;
import RIKU.server.Entity.User.User;
import RIKU.server.Entity.User.UserPoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface UserPointRepository extends JpaRepository<UserPoint, Long> {
    // 유저 출석 내역 중복 확인
    boolean existsByUserAndPointTypeAndCreatedAtBetween(User user, PointType pointType, LocalDateTime start, LocalDateTime end);

    // 유저 포인트 총합
    @Query("SELECT COALLESCE(SUM(up.point), 0) FROM UserPoint up WHERE up.user = :user")
    int sumPointsByUser(@Param("user") User user);
}
