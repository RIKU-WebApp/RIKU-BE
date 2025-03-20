package RIKU.server.Repository;

import RIKU.server.Entity.Board.Post.Post;
import RIKU.server.Entity.Participant.Participant;
import RIKU.server.Entity.User.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ParticipantRepository extends JpaRepository<Participant, Long> {
    Boolean existsByPostAndUser(Post post, User user);

    List<Participant> findByPost(Post post);

    // post_id에 대한 참여자 수 카운트
    @Query("SELECT COUNT(p) FROM Participant p WHERE p.post.id = :postId")
    int countByPostId(@Param("postId") Long postId);
}
