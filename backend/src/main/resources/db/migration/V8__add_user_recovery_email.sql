ALTER TABLE users
ADD COLUMN IF NOT EXISTS recovery_email VARCHAR(255);

CREATE INDEX IF NOT EXISTS idx_users_recovery_email
ON users(recovery_email);
