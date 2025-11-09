DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM users WHERE email IS NULL OR email = '') THEN
        RAISE EXCEPTION 'Има users без email! Моля, коригирайте преди да продължите.';
    END IF;
END $$;

ALTER TABLE users DROP CONSTRAINT IF EXISTS uk_username;
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_username_key;
DROP INDEX IF EXISTS idx_users_username;

ALTER TABLE users DROP COLUMN IF EXISTS username;

ALTER TABLE users ALTER COLUMN email SET NOT NULL;

DROP INDEX IF EXISTS idx_users_email;
CREATE UNIQUE INDEX idx_users_email ON users(email);