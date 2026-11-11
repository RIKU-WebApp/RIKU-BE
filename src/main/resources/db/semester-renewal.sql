CREATE TABLE IF NOT EXISTS semester_renewal (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    term VARCHAR(6) NOT NULL,
    member_id BIGINT NOT NULL,
    confirmed_by BIGINT NOT NULL,
    confirmed_at DATETIME(6) NOT NULL,
    UNIQUE KEY uk_renewal_term_member (term, member_id),
    FOREIGN KEY (member_id) REFERENCES users(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
