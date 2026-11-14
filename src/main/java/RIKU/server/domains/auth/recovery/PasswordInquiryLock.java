package RIKU.server.domains.auth.recovery;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
class PasswordInquiryLock {
    @Id
    private Integer slot;
}
