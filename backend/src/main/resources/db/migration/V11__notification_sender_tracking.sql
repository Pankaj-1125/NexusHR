/* NexusHR V11 - sender identity for auditable employee communications */
ALTER TABLE notifications
    ADD COLUMN IF NOT EXISTS sender_username VARCHAR(100);

ALTER TABLE notifications
    ADD COLUMN IF NOT EXISTS sender_role VARCHAR(50);

UPDATE notifications
SET sender_username = COALESCE(NULLIF(sender_username, ''), 'SYSTEM'),
    sender_role = COALESCE(NULLIF(sender_role, ''), 'SYSTEM');

ALTER TABLE notifications
    ALTER COLUMN sender_username SET DEFAULT 'SYSTEM';

ALTER TABLE notifications
    ALTER COLUMN sender_role SET DEFAULT 'SYSTEM';
