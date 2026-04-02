ALTER TABLE parameters ADD COLUMN IF NOT EXISTS filter_order INTEGER DEFAULT NULL;

CREATE INDEX IF NOT EXISTS idx_parameters_filter_order
ON parameters(filter_order);