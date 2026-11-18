-- Distinguish drinks from food and add-ons so cross-sell rules cannot be used as drink upgrades.
-- Existing categories default to DRINK; classify bakery/food/add-on categories in Admin after applying.
BEGIN;

ALTER TABLE public.categories
    ADD COLUMN IF NOT EXISTS product_type VARCHAR(20);

UPDATE public.categories SET product_type = 'DRINK' WHERE product_type IS NULL;

ALTER TABLE public.categories
    ALTER COLUMN product_type SET DEFAULT 'DRINK',
    ALTER COLUMN product_type SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'public.categories'::regclass
          AND conname = 'ck_categories_product_type'
    ) THEN
        ALTER TABLE public.categories
            ADD CONSTRAINT ck_categories_product_type
            CHECK (product_type IN ('DRINK', 'FOOD', 'ADD_ON'));
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_categories_product_type
    ON public.categories (product_type, del_flag);

COMMIT;
