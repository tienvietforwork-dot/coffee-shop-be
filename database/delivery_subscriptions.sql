-- Gói giao cà phê định kỳ (mỗi ngày / các thứ trong tuần, giờ giao cố định): hệ thống tự tạo đơn giao trước giờ giao,
-- khách trả khi nhận. Màn hình lịch hẹn giao + thứ tự pha chế ưu tiên.
-- Chạy tay trên Neon (an toàn khi chạy lại).
BEGIN;

CREATE TABLE IF NOT EXISTS delivery_subscriptions (
    id            BIGSERIAL PRIMARY KEY,
    code          VARCHAR(20)  NOT NULL,
    customer_id   BIGINT       NOT NULL REFERENCES customers (id),
    address_id    BIGINT       NOT NULL REFERENCES customer_addresses (id),
    frequency     VARCHAR(10)  NOT NULL CHECK (frequency IN ('DAILY', 'WEEKLY')),
    days          VARCHAR(20),                                  -- WEEKLY: 1 (thứ 2) … 7 (CN), dấu phẩy
    delivery_time VARCHAR(5)   NOT NULL,                        -- 'HH:mm' giờ VN, giờ giao cố định
    start_date    DATE         NOT NULL,
    end_date      DATE,                                         -- NULL = không hạn
    status        VARCHAR(10)  NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'PAUSED', 'CANCELLED')),
    note          TEXT,
    last_run_date DATE,                                         -- ngày gần nhất đã tạo (thử tạo) đơn
    last_error    VARCHAR(255),                                 -- lỗi lần tạo đơn gần nhất
    created_by VARCHAR(50)  NOT NULL DEFAULT 'system',
    created_at TIMESTAMP    NOT NULL DEFAULT now(),
    updated_by VARCHAR(50),
    updated_at TIMESTAMP,
    del_flag   BOOLEAN      NOT NULL DEFAULT FALSE,
    del_user   VARCHAR(50)
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_delivery_subscriptions_code ON delivery_subscriptions (code) WHERE NOT del_flag;
CREATE INDEX IF NOT EXISTS idx_delivery_subscriptions_customer ON delivery_subscriptions (customer_id);

CREATE TABLE IF NOT EXISTS delivery_subscription_items (
    id              BIGSERIAL PRIMARY KEY,
    subscription_id BIGINT       NOT NULL REFERENCES delivery_subscriptions (id) ON DELETE CASCADE,
    coffee_id       BIGINT       NOT NULL REFERENCES coffees (id),
    quantity        INT          NOT NULL CHECK (quantity BETWEEN 1 AND 99),
    note            VARCHAR(255)
);

-- đơn tự tạo từ gói
ALTER TABLE orders ADD COLUMN IF NOT EXISTS subscription_id BIGINT REFERENCES delivery_subscriptions (id);
CREATE INDEX IF NOT EXISTS idx_orders_subscription ON orders (subscription_id) WHERE subscription_id IS NOT NULL;

INSERT INTO permissions (code, name, path, module, description, sort_order)
SELECT v.code, v.name, v.path, 'core', v.description, v.sort_order FROM (VALUES
 ('delivery-schedule', 'Lịch hẹn giao & pha chế', '/admin/delivery-schedule', 'Đơn hẹn giờ trong ngày, đơn cần pha trước xếp đầu', 15),
 ('subscriptions',     'Gói giao định kỳ',        '/admin/subscriptions',     'Đăng ký / tạm dừng / hủy gói giao cà phê định kỳ cho khách', 41)
) AS v(code, name, path, description, sort_order)
WHERE NOT EXISTS (SELECT 1 FROM permissions p WHERE p.code = v.code AND NOT p.del_flag);

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON p.code IN ('delivery-schedule', 'subscriptions') AND NOT p.del_flag
WHERE r.code IN ('ADMIN', 'STAFF')
  AND NOT EXISTS (SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id AND NOT rp.del_flag);

COMMIT;
