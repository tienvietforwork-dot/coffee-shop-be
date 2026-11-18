-- Add the image fields already used by the admin FE combo form.
ALTER TABLE public.product_bundles
    ADD COLUMN IF NOT EXISTS image_id BIGINT;

ALTER TABLE public.product_bundles
    ADD COLUMN IF NOT EXISTS image_url VARCHAR(500);
