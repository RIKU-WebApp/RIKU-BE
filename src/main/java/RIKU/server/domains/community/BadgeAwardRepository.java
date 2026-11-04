package RIKU.server.domains.community;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface BadgeAwardRepository extends JpaRepository<BadgeAward,Long> {
    List<BadgeAward> findByUserIdOrderByIdAsc(Long id);
    boolean existsByUserIdAndName(Long userId,String name);
}
