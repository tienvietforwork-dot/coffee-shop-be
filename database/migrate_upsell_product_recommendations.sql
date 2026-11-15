BEGIN;

-- Optional manager note for product-to-product recommendations. Recommendation
-- eligibility is based on the live menu items and active recipe, not this note.
ALTER TABLE public.upsell_variant_links
    ADD COLUMN IF NOT EXISTS experience_note TEXT;

-- Seed a taste-matched example: both drinks are black coffee without milk,
-- with different brewing methods and flavor profiles.
INSERT INTO public.upsell_variant_links
    (base_coffee_id, variant_coffee_id, upgrade_type, enabled, experience_note)
SELECT base.id, premium.id, 'PREMIUM_VERSION', TRUE,
       'Cùng gu cà phê đen, không sữa; ủ lạnh 18 giờ cho hậu vị chocolate và cảm giác êm dịu.'
FROM public.coffees base
JOIN public.coffees premium ON lower(trim(premium.name)) = lower('Cold Brew truyền thống')
WHERE lower(trim(base.name)) = lower('Americano')
  AND base.id <> premium.id
  AND base.del_flag = FALSE
  AND premium.del_flag = FALSE
  AND premium.status = 'AVAILABLE'
  AND premium.auto_sold_out = FALSE
  AND EXISTS (
      SELECT 1 FROM public.recipes recipe
      WHERE recipe.coffee_id = premium.id
        AND recipe.is_active = TRUE AND recipe.del_flag = FALSE
  )
ON CONFLICT (base_coffee_id, variant_coffee_id, upgrade_type)
DO UPDATE SET experience_note = CASE
    WHEN nullif(trim(public.upsell_variant_links.experience_note), '') IS NULL
        THEN EXCLUDED.experience_note
    ELSE public.upsell_variant_links.experience_note
END;

-- Add an active example rule only when the pair is enabled and no rule for
-- this exact pair already exists. Existing admin choices are preserved.
INSERT INTO public.upsell_rules
    (name, anchor_coffee_id, upgrade_coffee_id, upgrade_type, priority,
     min_cart_amount, status, created_at)
SELECT 'Đề xuất món: Americano và Cold Brew truyền thống', base.id, premium.id,
       'PREMIUM_VERSION', 100, 0, 'ACTIVE', now()
FROM public.coffees base
JOIN public.coffees premium ON lower(trim(premium.name)) = lower('Cold Brew truyền thống')
JOIN public.upsell_variant_links link
  ON link.base_coffee_id = base.id
 AND link.variant_coffee_id = premium.id
 AND link.upgrade_type = 'PREMIUM_VERSION'
 AND link.enabled = TRUE
WHERE lower(trim(base.name)) = lower('Americano')
  AND base.del_flag = FALSE
  AND premium.del_flag = FALSE
  AND premium.status = 'AVAILABLE'
  AND premium.auto_sold_out = FALSE
  AND EXISTS (
      SELECT 1 FROM public.recipes recipe
      WHERE recipe.coffee_id = premium.id
        AND recipe.is_active = TRUE AND recipe.del_flag = FALSE
  )
  AND NOT EXISTS (
      SELECT 1
      FROM public.upsell_rules existing
      WHERE existing.anchor_coffee_id = base.id
        AND existing.upgrade_coffee_id = premium.id
        AND existing.upgrade_type = 'PREMIUM_VERSION'
  );

COMMIT;
