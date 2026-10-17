/*
 * ============================================================
 * NexusHR
 * V9 - Permanent employee deletion
 * ============================================================
 *
 * All foreign keys pointing to employees(id)
 * are changed to ON DELETE CASCADE.
 *
 * This allows Admin / Manager permanent deletion
 * to remove dependent employee data automatically.
 */

DO $$
DECLARE
fk RECORD;
    fk_definition TEXT;
BEGIN

FOR fk IN

SELECT
    c.oid,
    c.conname,
    c.conrelid::regclass AS child_table

FROM pg_constraint c

WHERE c.contype = 'f'

  AND c.confrelid =
      'employees'::regclass

    LOOP

        fk_definition :=
                pg_get_constraintdef(
                        fk.oid
                );

/*
 * Remove any existing ON DELETE rule.
 */
fk_definition :=
                regexp_replace(
                        fk_definition,
                        ' ON DELETE (NO ACTION|RESTRICT|CASCADE|SET NULL|SET DEFAULT)',
                        '',
                        'i'
                );

EXECUTE format(
        'ALTER TABLE %s DROP CONSTRAINT %I',
        fk.child_table,
        fk.conname
        );

EXECUTE format(
        'ALTER TABLE %s ADD CONSTRAINT %I %s ON DELETE CASCADE',
        fk.child_table,
        fk.conname,
        fk_definition
        );

END LOOP;

END $$;