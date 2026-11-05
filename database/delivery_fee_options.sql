-- Phí giao hàng theo "option giá": mỗi option có tên và các khoảng km → phí; option mặc định luôn có, áp dụng cho mọi giờ
-- không thuộc khung giờ nào. Khung giờ đặc biệt (giờ bắt đầu–kết thúc, ngày trong tuần) chọn 1 option; option dùng lại được.
-- Thay cho khung giờ "+đ / +%" (delivery_peak_hours) của delivery_time_slots.sql. Chạy sau file đó; chạy được nhiều lần.
BEGIN;

CREATE TABLE IF NOT EXISTS delivery_fee_plans (
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(60) NOT NULL,
    is_default BOOLEAN     NOT NULL DEFAULT FALSE
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_delivery_fee_plans_default ON delivery_fee_plans (is_default) WHERE is_default;

INSERT INTO delivery_fee_plans (name, is_default)
SELECT 'Mặc định', TRUE WHERE NOT EXISTS (SELECT 1 FROM delivery_fee_plans WHERE is_default);

-- các khoảng km hiện có thuộc option mặc định
ALTER TABLE delivery_fee_tiers ADD COLUMN IF NOT EXISTS plan_id BIGINT REFERENCES delivery_fee_plans (id);
UPDATE delivery_fee_tiers SET plan_id = (SELECT id FROM delivery_fee_plans WHERE is_default) WHERE plan_id IS NULL;
ALTER TABLE delivery_fee_tiers ALTER COLUMN plan_id SET NOT NULL;
CREATE INDEX IF NOT EXISTS idx_delivery_fee_tiers_plan ON delivery_fee_tiers (plan_id);

CREATE TABLE IF NOT EXISTS delivery_time_slots (
    id         BIGSERIAL PRIMARY KEY,
    start_time VARCHAR(5)  NOT NULL,                         -- 'HH:mm' giờ VN
    end_time   VARCHAR(5)  NOT NULL,                         -- < start_time = qua nửa đêm
    days       VARCHAR(20),                                  -- 1 (thứ 2) … 7 (CN), dấu phẩy; NULL = mọi ngày
    plan_id    BIGINT      NOT NULL REFERENCES delivery_fee_plans (id),
    CHECK (start_time <> end_time)
);

-- mẫu: option "Cao điểm" cho 6–8h và 17–20h
INSERT INTO delivery_fee_plans (name, is_default)
SELECT 'Cao điểm', FALSE WHERE NOT EXISTS (SELECT 1 FROM delivery_fee_plans WHERE NOT is_default)
                           AND NOT EXISTS (SELECT 1 FROM delivery_time_slots);
INSERT INTO delivery_fee_tiers (plan_id, from_km, to_km, fee)
SELECT p.id, v.from_km, v.to_km, v.fee
FROM delivery_fee_plans p, (VALUES (0, 3, 20000), (3, 5, 25000), (5, 8, 35000), (8, 12, 45000)) AS v(from_km, to_km, fee)
WHERE p.name = 'Cao điểm' AND NOT EXISTS (SELECT 1 FROM delivery_fee_tiers t WHERE t.plan_id = p.id);
INSERT INTO delivery_time_slots (start_time, end_time, plan_id)
SELECT v.s, v.e, p.id FROM delivery_fee_plans p, (VALUES ('06:00', '08:00'), ('17:00', '20:00')) AS v(s, e)
WHERE p.name = 'Cao điểm' AND NOT EXISTS (SELECT 1 FROM delivery_time_slots);

DROP TABLE IF EXISTS delivery_peak_hours;

COMMIT;
