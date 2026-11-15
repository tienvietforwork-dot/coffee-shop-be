-- Product Bundle: uploaded/external cover image and mutually exclusive customer choices.
-- Run manually on the shared PostgreSQL database used by app-stats and app-core.
BEGIN;

ALTER TABLE public.product_bundles
    ADD COLUMN IF NOT EXISTS image_id BIGINT,
    ADD COLUMN IF NOT EXISTS image_url VARCHAR(500);

ALTER TABLE public.product_bundle_items
    ADD COLUMN IF NOT EXISTS choice_group VARCHAR(80);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'public.product_bundles'::regclass
          AND conname = 'fk_product_bundles_image'
    ) THEN
        ALTER TABLE public.product_bundles
            ADD CONSTRAINT fk_product_bundles_image
            FOREIGN KEY (image_id) REFERENCES public.images (id);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'public.product_bundle_items'::regclass
          AND conname = 'ck_product_bundle_choice_group'
    ) THEN
        ALTER TABLE public.product_bundle_items
            ADD CONSTRAINT ck_product_bundle_choice_group
            CHECK (choice_group IS NULL OR (optional = TRUE AND length(btrim(choice_group)) > 0));
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_product_bundle_items_choice_group
    ON public.product_bundle_items (bundle_id, choice_group)
    WHERE del_flag = FALSE AND choice_group IS NOT NULL;

COMMIT;
