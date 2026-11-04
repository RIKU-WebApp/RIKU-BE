-- Apply to the application database before deploying the backend.
-- Existing posts have no recruitment row and retain their existing roster.
CREATE TABLE IF NOT EXISTS run_recruitment (
    post_id BIGINT PRIMARY KEY,
    capacity INT NULL,
    FOREIGN KEY (post_id) REFERENCES post(post_id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS run_recruitment_group (
    post_id BIGINT NOT NULL,
    group_order INT NOT NULL,
    group_name VARCHAR(255) NOT NULL,
    pace VARCHAR(255) NOT NULL,
    distance_km DECIMAL(7,2) NOT NULL,
    PRIMARY KEY (post_id, group_order),
    FOREIGN KEY (post_id) REFERENCES run_recruitment(post_id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS run_enrollment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    post_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role VARCHAR(16) NOT NULL,
    state VARCHAR(16) NOT NULL,
    group_name VARCHAR(255) NOT NULL,
    registered_at TIMESTAMP(6) NOT NULL,
    UNIQUE KEY uq_run_enrollment_member (post_id, user_id),
    KEY ix_run_enrollment_queue (post_id, state, id),
    FOREIGN KEY (post_id) REFERENCES run_recruitment(post_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(user_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS personal_run_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    date DATE NOT NULL,
    distance_km DECIMAL(7,2) NOT NULL,
    minutes INT NOT NULL,
    title VARCHAR(80) NOT NULL,
    KEY ix_personal_run_date (user_id, date, id),
    FOREIGN KEY (user_id) REFERENCES users(user_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS badge_award (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    name VARCHAR(40) NOT NULL,
    description VARCHAR(200),
    awarded_by BIGINT NOT NULL,
    awarded_at TIMESTAMP(6) NOT NULL,
    UNIQUE KEY uq_badge_award_member_name (user_id, name),
    FOREIGN KEY (user_id) REFERENCES users(user_id)
) ENGINE=InnoDB;
