-- Nguyên liệu chỉ gồm nguyên liệu thô: công thức đang dùng bán thành phẩm (cốt cà phê, cốt cold brew, nước đường,
-- siro sả, kem muối) được quy đổi ra nguyên liệu thô, rồi xóa (mềm) các bán thành phẩm và nguyên liệu trùng.
--   Cốt cà phê phin 1 ml  → Cà phê Robusta rang xay 0,5 g
--   Cốt cold brew   1 ml  → Hạt Arabica 0,125 g
--   Nước đường     15 ml  → Đường 10 g
--   Siro sả        15 ml  → Sả cây 5 g + Đường 10 g
--   Kem muối       40 ml  → Whipping cream 25 ml + Sữa tươi 15 ml + Muối 0,5 g
-- Chạy tay trên Neon (chạy lại không đổi gì: các dòng bán thành phẩm đã bị xóa).
BEGIN;

CREATE TEMP TABLE raw_conversion (semi VARCHAR(100), raw VARCHAR(100), factor NUMERIC(10,4)) ON COMMIT DROP;
INSERT INTO raw_conversion VALUES
 ('Cốt cà phê phin', 'Cà phê Robusta rang xay', 0.5),
 ('Cốt cold brew',   'Hạt Arabica',             0.125),
 ('Nước đường',      'Đường',                   0.6667),
 ('Siro sả',         'Sả cây',                  0.3333),
 ('Siro sả',         'Đường',                   0.6667),
 ('Kem muối',        'Whipping cream',          0.625),
 ('Kem muối',        'Sữa tươi',                0.375),
 ('Kem muối',        'Muối',                    0.0125);

-- nguyên liệu thô còn thiếu thì thêm (số lượng 0)
INSERT INTO materials (name, unit, created_by)
SELECT DISTINCT c.raw, CASE WHEN c.raw IN ('Whipping cream', 'Sữa tươi') THEN 'ml' ELSE 'g' END, 'migration'
FROM raw_conversion c
WHERE NOT EXISTS (SELECT 1 FROM materials m WHERE lower(m.name) = lower(c.raw) AND NOT m.del_flag);

-- thêm dòng nguyên liệu thô vào mọi công thức đang dùng bán thành phẩm (đã có thì cộng dồn)
INSERT INTO recipe_materials (recipe_id, material_id, quantity, note, created_by)
SELECT rm.recipe_id, raw.id, round(sum(rm.quantity * c.factor), 1), NULL, 'migration'
FROM recipe_materials rm
JOIN materials semi ON semi.id = rm.material_id AND NOT semi.del_flag
JOIN raw_conversion c ON lower(c.semi) = lower(semi.name)
JOIN materials raw ON lower(raw.name) = lower(c.raw) AND NOT raw.del_flag
WHERE NOT rm.del_flag
GROUP BY rm.recipe_id, raw.id
ON CONFLICT (recipe_id, material_id) WHERE NOT del_flag
DO UPDATE SET quantity = recipe_materials.quantity + EXCLUDED.quantity, updated_by = 'migration', updated_at = now();

-- bỏ dòng bán thành phẩm
UPDATE recipe_materials rm SET del_flag = TRUE, del_user = 'migration', updated_at = now()
FROM materials semi
WHERE semi.id = rm.material_id AND NOT rm.del_flag
  AND lower(semi.name) IN (SELECT DISTINCT lower(semi) FROM raw_conversion);

-- xóa bán thành phẩm và nguyên liệu trùng (không công thức nào còn dùng)
UPDATE materials m SET del_flag = TRUE, del_user = 'migration', updated_at = now()
WHERE NOT m.del_flag
  AND (lower(m.name) IN (SELECT DISTINCT lower(semi) FROM raw_conversion) OR lower(m.name) = lower('Sữa tươi bóc ra'))
  AND NOT EXISTS (SELECT 1 FROM recipe_materials rm WHERE rm.material_id = m.id AND NOT rm.del_flag);

COMMIT;
