-- Ước tính giờ giao tới: thời gian di chuyển (phút / km) × quãng đường quán → khách. Chạy được nhiều lần.
BEGIN;
ALTER TABLE delivery_settings ADD COLUMN IF NOT EXISTS minutes_per_km NUMERIC(4,1) CHECK (minutes_per_km > 0);
UPDATE delivery_settings SET minutes_per_km = 3 WHERE id = 1 AND minutes_per_km IS NULL;
COMMIT;
