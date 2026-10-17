/*
 * NexusHR V10 - Notification integrity
 *
 * Remove legacy orphan rows and enforce that notifications are deleted
 * automatically when an employee is permanently deleted.
 */

DELETE FROM notifications n
WHERE NOT EXISTS (
    SELECT 1
    FROM employees e
    WHERE e.id = n.employee_id
);

DO $$
DECLARE
    existing_fk RECORD;
BEGIN
    FOR existing_fk IN
        SELECT c.conname
        FROM pg_constraint c
        WHERE c.contype = 'f'
          AND c.conrelid = 'notifications'::regclass
          AND c.confrelid = 'employees'::regclass
    LOOP
        EXECUTE format(
            'ALTER TABLE notifications DROP CONSTRAINT %I',
            existing_fk.conname
        );
    END LOOP;
END $$;

ALTER TABLE notifications
    ADD CONSTRAINT fk_notifications_employee
    FOREIGN KEY (employee_id)
    REFERENCES employees(id)
    ON DELETE CASCADE;

CREATE INDEX IF NOT EXISTS idx_notifications_employee_created
    ON notifications(employee_id, created_at DESC);
