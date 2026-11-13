-- Product Placement: allow placing a coffee at the top of a chosen menu category.
-- Run manually against the shared PostgreSQL database before deploying the matching BE/FE code.
BEGIN;

ALTER TABLE product_placement_rules
    ADD COLUMN IF NOT EXISTS target_category_id BIGINT;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'product_placement_rules'::regclass
          AND conname = 'fk_product_placement_target_category'
    ) THEN
        ALTER TABLE product_placement_rules
            ADD CONSTRAINT fk_product_placement_target_category
            FOREIGN KEY (target_category_id) REFERENCES categories (id);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'product_placement_rules'::regclass
          AND conname = 'ck_product_placement_target_category'
    ) THEN
        ALTER TABLE product_placement_rules
            ADD CONSTRAINT ck_product_placement_target_category
            CHECK (
                (section = 'CATEGORY' AND target_category_id IS NOT NULL)
                OR (section <> 'CATEGORY' AND target_category_id IS NULL)
            );
    END IF;
END $$;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM product_placement_rules
        WHERE del_flag = false
        GROUP BY coffee_id, section, COALESCE(target_category_id, 0)
        HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION 'Duplicate active Product Placement rows exist; resolve them before adding the unique index.';
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_product_placement_target_category
    ON product_placement_rules (target_category_id)
    WHERE target_category_id IS NOT NULL AND del_flag = false;

-- Null category means a global zone; use 0 as a safe sentinel because category ids are positive.
-- This also closes the duplicate-check race between simultaneous create requests.
CREATE UNIQUE INDEX IF NOT EXISTS uq_product_placement_coffee_section_target
    ON product_placement_rules (coffee_id, section, (COALESCE(target_category_id, 0)))
    WHERE del_flag = false;

-- The section CHECK generated earlier only lists the five original sections; allow CATEGORY.
ALTER TABLE product_placement_rules DROP CONSTRAINT IF EXISTS product_placement_rules_section_check;
ALTER TABLE product_placement_rules ADD CONSTRAINT product_placement_rules_section_check
    CHECK (section IN ('FEATURED', 'BEST_SELLER', 'NEW_ARRIVAL', 'RECOMMENDED', 'HOME', 'CATEGORY'));

COMMIT;
