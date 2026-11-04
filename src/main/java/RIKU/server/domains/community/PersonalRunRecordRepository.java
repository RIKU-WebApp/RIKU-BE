package RIKU.server.domains.community;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface PersonalRunRecordRepository extends JpaRepository<PersonalRunRecord,Long> {
    List<PersonalRunRecord> findByUserIdOrderByDateDescIdDesc(Long id);
}
