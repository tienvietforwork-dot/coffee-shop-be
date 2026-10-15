-- Lịch sử chuyển trạng thái đơn hàng: mỗi lần đổi trạng thái 1 dòng (from_status NULL = lúc đặt đơn).
-- PHẢI chạy trước khi deploy app-core có bảng này.
CREATE TABLE IF NOT EXISTS order_status_history (
    id          BIGSERIAL PRIMARY KEY,
    order_id    BIGINT      NOT NULL REFERENCES orders (id),
    from_status VARCHAR(20),
    to_status   VARCHAR(20) NOT NULL,
    staff_id    BIGINT      REFERENCES staff (id),
    note        VARCHAR(255),
    created_by VARCHAR(50)  NOT NULL DEFAULT 'system',
    created_at TIMESTAMP    NOT NULL DEFAULT now(),
    updated_by VARCHAR(50),
    updated_at TIMESTAMP,
    del_flag   BOOLEAN      NOT NULL DEFAULT FALSE,
    del_user   VARCHAR(50)
);
CREATE INDEX IF NOT EXISTS idx_order_status_history_order ON order_status_history (order_id);
