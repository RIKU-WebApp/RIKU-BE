package RIKU.server.Repository;

import RIKU.server.Entity.Board.Post.Post;
import RIKU.server.Entity.Participant.Participant;
import RIKU.server.Entity.Participant.ParticipantStatus;
import RIKU.server.Entity.User.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ParticipantRepository extends JpaRepository<Participant, Long> {
    // 특정 유저의 해당 게시글 참여 중복 확인
    Boolean existsByPostAndUser(Post post, User user);

    // 특정 게시글에서 특정 유저의 참여 내역 조회
    Optional<Participant> findByPostAndUser(Post post, User user);

    // 특정 게시글의 모든 참여자 리스트 조회
    List<Participant> findByPost(Post post);

    // 특정 postId에 대한 참여자 수 조회
    @Query("SELECT COUNT(p) FROM Participant p WHERE p.post.id = :postId")
    int countByPostId(@Param("postId") Long postId);

    // 특정 유저가 특정 상태로 참여한 횟수 조회
    int countByUserAndParticipantStatus(User user, ParticipantStatus participantStatus);
}
