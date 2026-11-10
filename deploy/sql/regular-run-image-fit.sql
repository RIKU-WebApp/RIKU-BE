SET SESSION lock_wait_timeout=3;
SET @image_fit_ddl = IF(
  EXISTS (SELECT 1 FROM information_schema.columns
          WHERE table_schema = DATABASE() AND table_name = 'post' AND column_name = 'show_full_image'),
  'SELECT 1', 'ALTER TABLE post ADD COLUMN show_full_image BOOLEAN NULL, ALGORITHM=INSTANT'
);
PREPARE image_fit_stmt FROM @image_fit_ddl;
EXECUTE image_fit_stmt;
DEALLOCATE PREPARE image_fit_stmt;
SET SESSION lock_wait_timeout=DEFAULT;
