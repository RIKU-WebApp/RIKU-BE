package RIKU.server.domains.admin.renewal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface SemesterRenewalRepository extends JpaRepository<SemesterRenewal, Long> {
    @Query("select r.member.studentId from SemesterRenewal r where r.term = :term")
    List<String> confirmedStudents(@Param("term") String term);

    boolean existsByTermAndMember_Id(String term, Long memberId);
}
