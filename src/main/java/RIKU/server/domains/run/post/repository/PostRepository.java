package RIKU.server.domains.run.post.repository;

import RIKU.server.global.entity.BaseStatus;
import RIKU.server.domains.run.post.entity.Post;
import RIKU.server.domains.run.post.entity.PostType;
import RIKU.server.domains.run.post.entity.PostStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Post p where p.id = :id")
    Optional<Post> findForUpdate(@Param("id") Long id);

    @Query("select new RIKU.server.domains.run.post.repository.PostPreviewRow(p.id, p.title, p.date, p.postStatus, p.postImageUrl) " +
            "from Post p where p.status = :status and p.postType = :type and p.date >= :start and p.date < :end " +
            "order by case when p.postStatus = :openStatus then 0 else 1 end, p.date, p.id")
    List<PostPreviewRow> findTodayPreviews(@Param("status") BaseStatus status, @Param("type") PostType type,
            @Param("start") LocalDateTime start, @Param("end") LocalDateTime end, @Param("openStatus") PostStatus openStatus);

    @Query("select new RIKU.server.domains.run.post.repository.PostPreviewRow(p.id, p.title, p.date, p.postStatus, p.postImageUrl) " +
            "from Post p where p.status = :status and p.postType = :type and p.date >= :start order by p.date, p.id")
    List<PostPreviewRow> findUpcomingPreviews(@Param("status") BaseStatus status, @Param("type") PostType type,
            @Param("start") LocalDateTime start);

    @Query("select new RIKU.server.domains.run.post.repository.PostPreviewRow(p.id, p.title, p.date, p.postStatus, p.postImageUrl) " +
            "from Post p where p.status = :status and p.postType = :type and p.postStatus = :closedStatus " +
            "and p.date < :before order by p.date desc, p.id desc")
    List<PostPreviewRow> findPastPreviews(@Param("status") BaseStatus status, @Param("type") PostType type,
            @Param("closedStatus") PostStatus closedStatus, @Param("before") LocalDateTime before, Pageable pageable);

    List<Post> findByStatusAndPostType(BaseStatus baseStatus, PostType postType);

    // 해당 날짜 게시글 조회
    List<Post> findByStatusAndDateBetween(BaseStatus status, LocalDateTime startDate, LocalDateTime endDate);

    // 러닝 유형별 가장 가까운 날짜의 게시글 1개 조회
    Optional<Post> findTopByStatusAndPostTypeAndPostStatusOrderByDateAsc(BaseStatus status, PostType postType, PostStatus postStatus);

    // 같은 제목, 같은 작성자, 같은 날짜가 존재하는 게시글 조회
    Optional<Post> findByPostCreatorIdAndTitleAndDate(Long creatorId, String title, LocalDateTime date);

}
