SET SESSION lock_wait_timeout=3;
-- Additive schema for member feedback and private certificates.
CREATE TABLE IF NOT EXISTS feedback (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  category VARCHAR(32) NOT NULL,
  title VARCHAR(100) NOT NULL,
  content VARCHAR(3000) NOT NULL,
  status VARCHAR(32) NOT NULL,
  answer VARCHAR(2000) NULL,
  created_at DATETIME(6) NOT NULL,
  KEY ix_feedback_owner (user_id,id),
  FOREIGN KEY (user_id) REFERENCES users(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS run_certificate (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  title VARCHAR(120) NOT NULL,
  date DATE NOT NULL,
  image MEDIUMBLOB NOT NULL,
  distance_km DECIMAL(7,2) NULL,
  KEY ix_certificate_owner (user_id,id),
  FOREIGN KEY (user_id) REFERENCES users(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Existing archives predate the optional distance. This script also runs on subsequent starts.
SET @certificate_distance_ddl = IF(
  EXISTS (SELECT 1 FROM information_schema.columns
          WHERE table_schema = DATABASE() AND table_name = 'run_certificate' AND column_name = 'distance_km'),
  'SELECT 1', 'ALTER TABLE run_certificate ADD COLUMN distance_km DECIMAL(7,2) NULL'
);
PREPARE certificate_distance_stmt FROM @certificate_distance_ddl;
EXECUTE certificate_distance_stmt;
DEALLOCATE PREPARE certificate_distance_stmt;
SET SESSION lock_wait_timeout=DEFAULT;
