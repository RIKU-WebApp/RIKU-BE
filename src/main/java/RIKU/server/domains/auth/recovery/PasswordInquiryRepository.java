package RIKU.server.domains.auth.recovery;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;

interface PasswordInquiryRepository extends JpaRepository<PasswordInquiry, Long> {
    // InnoDB takes an exclusive row lock on both the insert and duplicate-update paths.
    @Modifying
    @Query(value = """
            INSERT INTO password_inquiry_lock (slot) VALUES (:slot)
            ON DUPLICATE KEY UPDATE slot = :slot
            """, nativeQuery = true)
    void lockSubmissionSlot(int slot);

    boolean existsByStudentIdAndNameAndMajorAndPhoneAndCreatedAtAfter(
            String studentId, String name, String major, String phone, Instant cutoff);
}
