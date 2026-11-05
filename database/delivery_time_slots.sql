-- Giờ mở cửa (giới hạn giờ hẹn nhận / giao) và khung giờ giá ship: giờ không thuộc khung nào = giá mặc định (bậc km);
-- mỗi khung (vd 17–20h, 6–8h) cộng thêm số tiền (AMOUNT) hoặc % (PERCENT) vào phí bậc km, có thể bỏ miễn phí ship.
-- Giờ lưu dạng chuỗi 'HH:mm' giờ Việt Nam (cột TIME bị lệch qua hibernate.jdbc.time_zone=UTC).
-- Chạy sau delivery_fee_by_distance.sql; chạy được nhiều lần.
BEGIN;

ALTER TABLE delivery_settings ADD COLUMN IF NOT EXISTS open_time  VARCHAR(5);   -- NULL = mở cả ngày
ALTER TABLE delivery_settings ADD COLUMN IF NOT EXISTS close_time VARCHAR(5);   -- < open_time = đóng sau nửa đêm

CREATE TABLE IF NOT EXISTS delivery_peak_hours (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(60)   NOT NULL,
    start_time      VARCHAR(5)    NOT NULL,                  -- 'HH:mm'
    end_time        VARCHAR(5)    NOT NULL,                  -- < start_time = qua nửa đêm
    days            VARCHAR(20),                             -- 1 (thứ 2) … 7 (CN), dấu phẩy; NULL = mọi ngày
    surcharge_type  VARCHAR(10)   NOT NULL CHECK (surcharge_type IN ('AMOUNT', 'PERCENT')),
    surcharge_value NUMERIC(12,2) NOT NULL CHECK (surcharge_value >= 0),
    no_free_ship    BOOLEAN       NOT NULL DEFAULT FALSE,    -- khung này không miễn phí ship
    active          BOOLEAN       NOT NULL DEFAULT TRUE,
    CHECK (start_time <> end_time)
);

-- bản chạy trước tạo cột TIME: đổi sang 'HH:mm'
ALTER TABLE delivery_settings   ALTER COLUMN open_time  TYPE VARCHAR(5) USING to_char(open_time::time,  'HH24:MI');
ALTER TABLE delivery_settings   ALTER COLUMN close_time TYPE VARCHAR(5) USING to_char(close_time::time, 'HH24:MI');
ALTER TABLE delivery_peak_hours DROP CONSTRAINT IF EXISTS delivery_peak_hours_check;
ALTER TABLE delivery_peak_hours ALTER COLUMN start_time TYPE VARCHAR(5) USING to_char(start_time::time, 'HH24:MI'),
                                ALTER COLUMN end_time   TYPE VARCHAR(5) USING to_char(end_time::time,   'HH24:MI');
ALTER TABLE delivery_peak_hours ADD CONSTRAINT delivery_peak_hours_check CHECK (start_time <> end_time);

UPDATE delivery_settings SET open_time = '06:00', close_time = '22:00' WHERE id = 1 AND open_time IS NULL;

INSERT INTO delivery_peak_hours (name, start_time, end_time, days, surcharge_type, surcharge_value, no_free_ship)
SELECT * FROM (VALUES
    ('Sáng sớm',     '06:00', '08:00', NULL::VARCHAR, 'AMOUNT',  5000::NUMERIC, FALSE),
    ('Cao điểm tối', '17:00', '20:00', NULL::VARCHAR, 'PERCENT', 30::NUMERIC,   TRUE)
) AS v(name, start_time, end_time, days, surcharge_type, surcharge_value, no_free_ship)
WHERE NOT EXISTS (SELECT 1 FROM delivery_peak_hours);

COMMIT;
