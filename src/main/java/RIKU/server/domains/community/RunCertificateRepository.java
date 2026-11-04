package RIKU.server.domains.community;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

interface RunCertificateRepository extends JpaRepository<RunCertificate, Long> {
    interface Summary {
        Long getId();
        String getTitle();
        LocalDate getDate();
    }
    List<Summary> findByUserIdOrderByIdDesc(Long userId);

    @Query("select c.image from RunCertificate c where c.id = :id and c.userId = :userId")
    Optional<byte[]> findImage(Long id, Long userId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @Query("delete from RunCertificate c where c.id = :id and c.userId = :userId")
    int deleteOwned(Long id, Long userId);
}
