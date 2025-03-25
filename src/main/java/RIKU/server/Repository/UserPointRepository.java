package RIKU.server.Repository;

import RIKU.server.Entity.User.PointType;
import RIKU.server.Entity.User.User;
import RIKU.server.Entity.User.UserPoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface UserPointRepository extends JpaRepository<UserPoint, Long> {
    boolean existsByUserAndPointTypeAndCreatedAtBetween(User user, PointType pointType, LocalDateTime start, LocalDateTime end);
}
