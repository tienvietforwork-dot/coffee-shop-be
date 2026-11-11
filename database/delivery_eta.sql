-- Ước tính giờ giao tới: quãng đường quán → khách ÷ tốc độ di chuyển (km/h). Chạy được nhiều lần.
-- Bản trước lưu phút / km (minutes_per_km): đổi sang km/h = 60 ÷ phút/km rồi bỏ cột cũ.
BEGIN;
ALTER TABLE delivery_settings ADD COLUMN IF NOT EXISTS speed_kmh NUMERIC(5,1) CHECK (speed_kmh > 0);
DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'delivery_settings' AND column_name = 'minutes_per_km') THEN
    UPDATE delivery_settings SET speed_kmh = ROUND(60 / minutes_per_km, 1) WHERE speed_kmh IS NULL AND minutes_per_km > 0;
    ALTER TABLE delivery_settings DROP COLUMN minutes_per_km;
  END IF;
END $$;
UPDATE delivery_settings SET speed_kmh = 20 WHERE id = 1 AND speed_kmh IS NULL;
COMMIT;
