package RIKU.server.Repository;

import RIKU.server.Entity.User.PointType;
import RIKU.server.Entity.User.User;
import RIKU.server.Entity.User.UserPoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UserPointRepository extends JpaRepository<UserPoint, Long> {
    // 유저 출석 내역 중복 확인
    boolean existsByUserAndPointTypeAndCreatedAtBetween(User user, PointType pointType, LocalDateTime start, LocalDateTime end);

    // 유저 포인트 총합
    @Query("SELECT COALESCE(SUM(up.point), 0) FROM UserPoint up WHERE up.user = :user")
    int sumPointsByUser(@Param("user") User user);

    // 해당 월 출석 현황 리스트 조회
    @Query("SELECT up.createdAt FROM UserPoint  up " +
            "WHERE up.user = :user " +
            "AND up.pointType = :pointType " +
            "AND up.createdAt BETWEEN :start AND :end")
    List<LocalDateTime> findAttendanceDatesInMonth(@Param("user") User user,
                                                   @Param("pointType") PointType pointType,
                                                   @Param("start") LocalDateTime start,
                                                   @Param("end") LocalDateTime end);
}
