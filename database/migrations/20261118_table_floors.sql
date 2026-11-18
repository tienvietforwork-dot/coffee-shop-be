-- Sơ đồ bàn ở màn order tại quầy: bàn nhóm theo tầng rồi theo khu vực.
-- Chạy tay trên Neon (an toàn khi chạy lại).
BEGIN;
ALTER TABLE dining_tables ADD COLUMN IF NOT EXISTS floor VARCHAR(50);
-- dữ liệu cũ: "Lầu 1" đang nằm ở cột khu vực → chuyển sang tầng; còn lại mặc định tầng trệt
UPDATE dining_tables SET floor = area, area = NULL WHERE floor IS NULL AND area ILIKE 'lầu%';
UPDATE dining_tables SET floor = 'Tầng trệt' WHERE floor IS NULL;
UPDATE dining_tables SET area = 'Trong nhà' WHERE area = 'Tầng trệt';
COMMIT;
