ALTER TABLE parameters
ADD COLUMN IF NOT EXISTS is_filter BOOLEAN NOT NULL DEFAULT TRUE;

CREATE INDEX IF NOT EXISTS idx_parameters_is_filter
ON parameters(is_filter);