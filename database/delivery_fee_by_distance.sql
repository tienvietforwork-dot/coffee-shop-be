-- Giao hàng: quán tự giao (bỏ đặt xe Ahamove / gọi xe ngoài), phí ship tính theo quãng đường quán → khách (km, qua API
-- bản đồ) và các khoảng km thiết lập trong màn "Giao hàng" (gộp "Khu vực giao hàng" vào). Đơn giao chỉ còn trạng thái:
-- chờ giao → đang giao → đã giao / giao không thành công.
-- Chạy được nhiều lần. Bảng delivery_zones và các cột Ahamove cũ của shipments giữ lại (không còn dùng) — xoá ở cuối file.
BEGIN;

CREATE TABLE IF NOT EXISTS delivery_settings (
    id                  BIGINT PRIMARY KEY CHECK (id = 1),     -- 1 dòng duy nhất
    enabled             BOOLEAN       NOT NULL DEFAULT TRUE,   -- false = tạm ngưng nhận đơn giao
    shop_address        VARCHAR(255)  NOT NULL,                -- điểm xuất phát để đo km
    shop_lat            DOUBLE PRECISION,
    shop_lng            DOUBLE PRECISION,
    free_ship_threshold NUMERIC(12,2) CHECK (free_ship_threshold >= 0), -- miễn phí ship từ
    min_order_amount    NUMERIC(12,2) CHECK (min_order_amount >= 0),    -- đơn tối thiểu để giao
    created_by VARCHAR(50)  NOT NULL DEFAULT 'system',
    created_at TIMESTAMP    NOT NULL DEFAULT now(),
    updated_by VARCHAR(50),
    updated_at TIMESTAMP,
    del_flag   BOOLEAN      NOT NULL DEFAULT FALSE,
    del_user   VARCHAR(50)
);

-- bậc phí: quãng đường trong (from_km, to_km] → phí; xa hơn bậc cuối = ngoài phạm vi giao
CREATE TABLE IF NOT EXISTS delivery_fee_tiers (
    id      BIGSERIAL PRIMARY KEY,
    from_km NUMERIC(6,2)  NOT NULL CHECK (from_km >= 0),
    to_km   NUMERIC(6,2)  NOT NULL,
    fee     NUMERIC(12,2) NOT NULL CHECK (fee >= 0),
    CHECK (to_km > from_km)
);

INSERT INTO delivery_settings (id, shop_address, shop_lat, shop_lng, free_ship_threshold, min_order_amount)
VALUES (1, 'Chợ Bến Thành, Quận 1, TP. Hồ Chí Minh', 10.7725, 106.6981, 200000, 50000)
ON CONFLICT (id) DO NOTHING;

INSERT INTO delivery_fee_tiers (from_km, to_km, fee)
SELECT * FROM (VALUES (0, 3, 15000), (3, 5, 20000), (5, 8, 30000), (8, 12, 40000)) AS v(from_km, to_km, fee)
WHERE NOT EXISTS (SELECT 1 FROM delivery_fee_tiers);

-- chuyến đang "đặt xe / tài xế nhận" kiểu cũ → chờ giao
UPDATE shipments SET status = 'PENDING' WHERE status IN ('BOOKED', 'DRIVER_ACCEPTED');

-- quyền 'delivery-zones' giữ mã, giờ là "Phí giao hàng" trong màn Giao hàng
UPDATE permissions SET name = 'Phí giao hàng', path = '/admin/shipments',
       description = 'Phí ship theo km, miễn phí từ, đơn tối thiểu, địa chỉ quán'
WHERE code = 'delivery-zones';
UPDATE permissions SET description = 'Đơn giao do quán tự giao: bắt đầu giao, đã giao, giao không thành công'
WHERE code = 'shipments';

COMMIT;

-- Dọn dữ liệu cũ (tuỳ chọn, không hoàn tác được — chạy khi mọi app đã lên bản mới):
-- DROP TABLE IF EXISTS delivery_zones;
-- ALTER TABLE shipments DROP COLUMN IF EXISTS district, DROP COLUMN IF EXISTS partner, DROP COLUMN IF EXISTS partner_order_id,
--     DROP COLUMN IF EXISTS partner_status, DROP COLUMN IF EXISTS partner_fee, DROP COLUMN IF EXISTS tracking_url,
--     DROP COLUMN IF EXISTS driver_name, DROP COLUMN IF EXISTS driver_phone, DROP COLUMN IF EXISTS carrier,
--     DROP COLUMN IF EXISTS tracking_code, DROP COLUMN IF EXISTS booked_at, DROP COLUMN IF EXISTS driver_accepted_at;
