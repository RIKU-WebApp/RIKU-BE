package RIKU.server.Repository;

import RIKU.server.Entity.Board.Post;
import RIKU.server.Entity.Participant.Participant;
import RIKU.server.Entity.User.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ParticipantRepository extends JpaRepository<Participant, Long> {
    Optional<Participant> findByPostAndUser(Post post, User user);

    List<Participant> findByPost(Post post);
}
