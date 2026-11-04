package RIKU.server.domains.point.repository;

import RIKU.server.domains.run.post.entity.Post;
import RIKU.server.domains.point.entity.PointType;
import RIKU.server.domains.user.entity.User;
import RIKU.server.domains.point.entity.UserPoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import RIKU.server.domains.point.ranking.RankingTotal;

@Repository
public interface UserPointRepository extends JpaRepository<UserPoint, Long> {
    List<UserPoint> findByPostIdAndUserIdIn(Long postId, List<Long> userIds);

    @Query("select new RIKU.server.domains.point.ranking.RankingTotal(up.user.id, sum(up.point)) " +
            "from UserPoint up where up.createdAt >= :start group by up.user.id")
    List<RankingTotal> sumByUserSince(@Param("start") LocalDateTime start);

    @Query("select new RIKU.server.domains.point.ranking.RankingTotal(up.user.id, sum(up.point)) " +
            "from UserPoint up where up.createdAt >= :start and up.createdAt < :end " +
            "and up.pointType in :types group by up.user.id")
    List<RankingTotal> sumByUserInEvent(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end,
                                      @Param("types") Set<PointType> types);
    // 유저 출석 내역 중복 확인
    boolean existsByUserAndPointTypeAndCreatedAtBetween(User user, PointType pointType, LocalDateTime start, LocalDateTime end);

    // 포인트 리스트 조회
    @EntityGraph(attributePaths = "post")
    List<UserPoint> findByUserAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(User user, LocalDateTime fromUtc);

    // 유저 포인트 총합
    @Query("SELECT COALESCE(SUM(up.point), 0) " +
            "FROM UserPoint up " +
            "WHERE up.user = :user " +
            "AND up.createdAt >= :fromUtc")
    int sumPointsByUserSince(@Param("user") User user, @Param("fromUtc") LocalDateTime fromUtc);

    // 해당 월 출석 현황 리스트 조회
    @Query("SELECT up.createdAt FROM UserPoint  up " +
            "WHERE up.user = :user " +
            "AND up.pointType = :pointType " +
            "AND up.createdAt BETWEEN :start AND :end")
    List<LocalDateTime> findAttendanceDatesInMonth(@Param("user") User user,
                                                   @Param("pointType") PointType pointType,
                                                   @Param("start") LocalDateTime start,
                                                   @Param("end") LocalDateTime end);

    // 기간별 유저 포인트 조회
    List<UserPoint> findByUserAndCreatedAtBetween(User user, LocalDateTime start, LocalDateTime end);

    List<UserPoint> deleteByPost(Post post);

    boolean existsByUserIdAndPostId(Long userId, Long postId);

    List<UserPoint> deleteByUserIdAndPostId(Long userId, Long postId);
}
