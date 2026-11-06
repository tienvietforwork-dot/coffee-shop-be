-- Align promotion discount storage with percentage and monetary discount types.
ALTER TABLE promotions
    ADD COLUMN IF NOT EXISTS discount_type VARCHAR(20);

UPDATE promotions
SET discount_type = 'PERCENTAGE'
WHERE discount_type IS NULL;

UPDATE promotions
SET discount_type = 'PERCENTAGE'
WHERE discount_type = 'SPECIAL'
  AND apply_scope = 'ORDER';

ALTER TABLE promotions
    ALTER COLUMN discount_type SET DEFAULT 'PERCENTAGE',
    ALTER COLUMN discount_type SET NOT NULL,
    ALTER COLUMN discount_percent TYPE NUMERIC(12,2),
    ALTER COLUMN discount_percent DROP NOT NULL;

DO $$
DECLARE
    constraint_name TEXT;
BEGIN
    FOR constraint_name IN
        SELECT conname
        FROM pg_constraint
        WHERE conrelid = 'promotions'::regclass
          AND contype = 'c'
          AND pg_get_constraintdef(oid) ILIKE '%discount_percent%'
    LOOP
        EXECUTE format('ALTER TABLE promotions DROP CONSTRAINT %I', constraint_name);
    END LOOP;
END $$;

ALTER TABLE promotions
    ADD CONSTRAINT promotions_discount_value_check CHECK (
        (discount_type = 'PERCENTAGE' AND (
            (apply_scope = 'ORDER' AND discount_percent IS NOT NULL AND discount_percent > 0 AND discount_percent <= 100)
            OR (apply_scope = 'COFFEE' AND (discount_percent IS NULL OR (discount_percent > 0 AND discount_percent <= 100)))
        ))
        OR (discount_type = 'FIXED_AMOUNT' AND (
            (apply_scope = 'ORDER' AND discount_percent IS NOT NULL AND discount_percent > 0)
            OR (apply_scope = 'COFFEE' AND (discount_percent IS NULL OR discount_percent > 0))
        ))
        OR (discount_type = 'SPECIAL' AND apply_scope = 'COFFEE'
            AND (discount_percent IS NULL OR discount_percent > 0))
    );
