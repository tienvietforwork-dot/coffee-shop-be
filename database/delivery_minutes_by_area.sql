-- Thời gian giao theo khu vực (thay cho km ÷ tốc độ km/h): mỗi tỉnh / thành một số phút giao, phường / xã có thể đặt
-- riêng — cấu hình giống phí ship. Đơn giao lưu lại số phút lúc đặt để lịch pha chế ổn định khi cấu hình đổi.
-- Chạy tay trên Neon (an toàn khi chạy lại).
BEGIN;
ALTER TABLE delivery_provinces ADD COLUMN IF NOT EXISTS delivery_minutes INTEGER NOT NULL DEFAULT 30 CHECK (delivery_minutes > 0);
ALTER TABLE delivery_wards     ADD COLUMN IF NOT EXISTS delivery_minutes INTEGER CHECK (delivery_minutes > 0);  -- NULL = theo tỉnh
ALTER TABLE shipments          ADD COLUMN IF NOT EXISTS delivery_minutes INTEGER;                               -- phút giao lúc đặt đơn
-- xã đã có phí riêng (thường là xa hơn): mặc định 45 phút, quán chỉnh lại sau
UPDATE delivery_wards SET delivery_minutes = 45 WHERE delivery_minutes IS NULL AND fee IS NOT NULL AND NOT blocked;
ALTER TABLE delivery_settings DROP COLUMN IF EXISTS speed_kmh;
COMMIT;
