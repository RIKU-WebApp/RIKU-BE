package RIKU.server.domains.community;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface BadgeAwardRepository extends JpaRepository<BadgeAward,Long> {
    List<BadgeAward> findByUserIdOrderByIdAsc(Long id);
    List<BadgeAward> findByUserIdOrderByIdAsc(Long id, org.springframework.data.domain.Pageable page);
    boolean existsByUserIdAndName(Long userId,String name);
}
