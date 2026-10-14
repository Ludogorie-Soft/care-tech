ALTER TABLE parameters
ADD COLUMN IF NOT EXISTS most_key VARCHAR(255);

CREATE INDEX IF NOT EXISTS idx_parameters_most_key
ON parameters(most_key);