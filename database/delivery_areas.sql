-- Phí giao hàng theo khu vực: chỉ giao tới các tỉnh / thành đã cấu hình (mặc định Thành phố Hà Nội), mỗi tỉnh một phí,
-- phường / xã có thể đặt phí riêng hoặc chặn. Bỏ hẳn phí theo km, khung giờ cao điểm và phụ phí thời tiết.
-- Khách thuộc nhóm CRM được chọn (Thân thiết, VIP) miễn phí giao hàng.
-- Chạy tay trên Neon (an toàn khi chạy lại).
BEGIN;

CREATE TABLE IF NOT EXISTS delivery_provinces (
    id     BIGSERIAL PRIMARY KEY,
    name   VARCHAR(100)  NOT NULL,                       -- "Thành phố Hà Nội" (danh mục 34 tỉnh / thành)
    fee    NUMERIC(12,2) NOT NULL CHECK (fee >= 0),      -- phí ship mặc định cho mọi phường / xã của tỉnh
    active BOOLEAN       NOT NULL DEFAULT TRUE           -- false = tạm ngưng giao tới tỉnh này
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_delivery_provinces_name ON delivery_provinces (lower(name));

-- phường / xã khác tỉnh: phí riêng (fee) hoặc không giao (blocked)
CREATE TABLE IF NOT EXISTS delivery_wards (
    id          BIGSERIAL PRIMARY KEY,
    province_id BIGINT        NOT NULL REFERENCES delivery_provinces (id) ON DELETE CASCADE,
    name        VARCHAR(100)  NOT NULL,
    fee         NUMERIC(12,2) CHECK (fee >= 0),          -- NULL = theo phí tỉnh
    blocked     BOOLEAN       NOT NULL DEFAULT FALSE
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_delivery_wards_name ON delivery_wards (province_id, lower(name));

-- nhóm khách CRM (customer_groups.id, phân cách dấu phẩy) được miễn phí giao hàng: Thân thiết, VIP…
ALTER TABLE delivery_settings ADD COLUMN IF NOT EXISTS free_ship_group_ids VARCHAR(255);

-- mặc định: chỉ giao trong Hà Nội, 15.000đ
INSERT INTO delivery_provinces (name, fee)
SELECT 'Thành phố Hà Nội', 15000
WHERE NOT EXISTS (SELECT 1 FROM delivery_provinces);

-- miễn phí ship cho nhóm khách tên "Thân thiết" / "VIP" nếu CRM đã có, khi chưa cấu hình gì
UPDATE delivery_settings SET free_ship_group_ids = (
    SELECT string_agg(id::text, ',' ORDER BY id) FROM customer_groups
    WHERE NOT del_flag AND (lower(name) LIKE '%vip%' OR lower(name) LIKE '%thân thiết%'))
WHERE free_ship_group_ids IS NULL;

-- bỏ phí theo km / khung giờ / thời tiết
DROP TABLE IF EXISTS delivery_time_slots;
DROP TABLE IF EXISTS delivery_fee_tiers;
DROP TABLE IF EXISTS delivery_fee_plans;
DROP TABLE IF EXISTS delivery_weather_tiers;
ALTER TABLE delivery_settings
    DROP COLUMN IF EXISTS weather_enabled,
    DROP COLUMN IF EXISTS weather_stop_rain_mm,
    DROP COLUMN IF EXISTS weather_note;

COMMIT;
