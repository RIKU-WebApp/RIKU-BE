-- Idempotent badge schema, applied by the prodDB profile before ledger discovery.
CREATE TABLE IF NOT EXISTS automatic_badge_grant (
  user_id BIGINT NOT NULL,
  badge_code VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  badge_name VARCHAR(120) NOT NULL,
  badge_rule VARCHAR(500) NOT NULL,
  badge_source VARCHAR(16) NOT NULL,
  granted_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (user_id,badge_code),
  FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Immutable activity evidence, separate from the original grant timestamp.
-- A NULL date means there was no reliably dated qualifying activity.
CREATE TABLE IF NOT EXISTS badge_activity_date (
  user_id BIGINT NOT NULL,
  badge_code VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  achieved_on DATE NULL,
  PRIMARY KEY(user_id,badge_code),
  FOREIGN KEY(user_id,badge_code) REFERENCES automatic_badge_grant(user_id,badge_code) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS rank_honor_period (
  kind VARCHAR(12) NOT NULL, starts_on DATE NOT NULL, ends_before DATE NOT NULL,
  settled BOOLEAN NOT NULL DEFAULT FALSE, PRIMARY KEY(kind,starts_on)
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS rank_honor (
  kind VARCHAR(12) NOT NULL, starts_on DATE NOT NULL, user_id BIGINT NOT NULL, place_no INT NOT NULL,
  issued_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY(kind,starts_on,user_id), INDEX(user_id),
  FOREIGN KEY(kind,starts_on) REFERENCES rank_honor_period(kind,starts_on),
  FOREIGN KEY(user_id) REFERENCES users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Evidence for records removed before reconciliation (including while issuance is paused).
CREATE TABLE IF NOT EXISTS badge_record_peak (
  user_id BIGINT NOT NULL PRIMARY KEY,
  personal_km DECIMAL(16,2) NOT NULL DEFAULT 0,
  had_certificate BOOLEAN NOT NULL DEFAULT FALSE,
  FOREIGN KEY(user_id) REFERENCES users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS badge_job_lock (
  job_name VARCHAR(32) NOT NULL PRIMARY KEY
) ENGINE=InnoDB;
INSERT INTO badge_job_lock(job_name) VALUES('catalog')
ON DUPLICATE KEY UPDATE job_name=badge_job_lock.job_name;

-- Observe configured season starts across restarts even while issuance is paused.
CREATE TABLE IF NOT EXISTS badge_semester_start (
  starts_on DATE NOT NULL PRIMARY KEY
) ENGINE=InnoDB;
