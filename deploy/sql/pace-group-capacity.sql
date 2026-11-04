-- Apply once before deploying. NULL preserves the existing unlimited group size.
ALTER TABLE run_recruitment_group ADD COLUMN group_capacity INT NULL;
