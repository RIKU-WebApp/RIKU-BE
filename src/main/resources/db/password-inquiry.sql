CREATE TABLE IF NOT EXISTS password_inquiry (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  student_id VARCHAR(255) NOT NULL,
  name VARCHAR(255) NOT NULL,
  major VARCHAR(255) NOT NULL,
  phone VARCHAR(11) NULL,
  status VARCHAR(32) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  KEY ix_password_inquiry_student_time (student_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS password_inquiry_lock (
  slot INT NOT NULL PRIMARY KEY
) ENGINE=InnoDB;
