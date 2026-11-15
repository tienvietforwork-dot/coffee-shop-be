-- Thời tiết xấu (Open-Meteo, tại quán, theo giờ giao): phụ phí theo bậc lượng mưa cộng vào phí ship mọi khung giờ;
-- ngưng nhận giao khi mưa từ … mm/h hoặc có giông. Chạy được nhiều lần.
BEGIN;

ALTER TABLE delivery_settings ADD COLUMN IF NOT EXISTS weather_enabled         BOOLEAN       NOT NULL DEFAULT FALSE;
ALTER TABLE delivery_settings ADD COLUMN IF NOT EXISTS weather_stop_rain_mm    NUMERIC(5,1)  CHECK (weather_stop_rain_mm > 0);

-- mưa từ min_rain_mm mm/h → cộng surcharge (lấy bậc cao nhất đạt)
CREATE TABLE IF NOT EXISTS delivery_weather_tiers (
    id          BIGSERIAL PRIMARY KEY,
    min_rain_mm NUMERIC(5,1)  NOT NULL CHECK (min_rain_mm > 0),
    surcharge   NUMERIC(12,2) NOT NULL CHECK (surcharge >= 0)
);

-- mẫu (đang tắt): mưa vừa +5.000đ, mưa to +10.000đ, ngưng giao khi mưa từ 30 mm/h
INSERT INTO delivery_weather_tiers (min_rain_mm, surcharge)
SELECT * FROM (VALUES (2.5, 5000), (8, 10000)) AS v(min_rain_mm, surcharge)
WHERE NOT EXISTS (SELECT 1 FROM delivery_weather_tiers);
UPDATE delivery_settings SET weather_stop_rain_mm = 30 WHERE id = 1 AND weather_stop_rain_mm IS NULL;

-- bản trước có phụ phí giông / ngưng giao khi giông: giờ giông luôn ngưng giao
ALTER TABLE delivery_settings DROP COLUMN IF EXISTS weather_storm_surcharge, DROP COLUMN IF EXISTS weather_stop_storm;

COMMIT;
