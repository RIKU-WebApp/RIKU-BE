package RIKU.server.domains.run.pacer.repository;

import RIKU.server.domains.run.pacer.entity.Pacer;
import RIKU.server.domains.run.post.entity.Post;
import RIKU.server.domains.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PacerRepository extends JpaRepository<Pacer, Long> {
    List<Pacer> findByPost(Post post);

    boolean existsByUserAndPost(User user, Post post);

    void deleteByPost(Post post);

    @Query("SELECT COUNT(p) FROM Pacer p WHERE p.post = :post")
    int countByPost(@Param("post") Post post);
}
