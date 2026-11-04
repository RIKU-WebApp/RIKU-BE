package RIKU.server.domains.run.post.repository;

import RIKU.server.domains.run.post.entity.Post;
import RIKU.server.domains.run.post.entity.RegularPost;
import RIKU.server.domains.run.participant.entity.Participant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RegularPostRepository extends JpaRepository<RegularPost, Long> {

    Optional<RegularPost> findByPost(Post post);

    interface CodeAndOrganizer {
        String getCode();
        Participant getOrganizer();
    }

    @Query("""
            select r.attendanceCode as code, p as organizer
            from RegularPost r
            left join RunRecruitment config on config.postId = r.id
            left join Participant p on p.post.id = config.postId and p.user.id = :organizerId
            where r.id = :postId
            """)
    Optional<CodeAndOrganizer> findCodeAndOrganizer(@Param("postId") Long postId,
                                                    @Param("organizerId") Long organizerId);

    void deleteByPost(Post post);
}
