package RIKU.server.domains.auth.recovery;

import org.springframework.data.jpa.repository.JpaRepository;

interface PasswordInquiryRepository extends JpaRepository<PasswordInquiry, Long> {}
