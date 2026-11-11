-- Số lượng nguyên liệu (dạng thô, không phiếu nhập/xuất, không tồn tối thiểu).
-- Đơn bắt đầu pha tự trừ theo công thức; hủy / từ chối sau đó thì cộng lại; món thiếu nguyên liệu tự "Hết món".
-- Chạy tay trên Neon (an toàn khi chạy lại).
BEGIN;

ALTER TABLE materials ADD COLUMN IF NOT EXISTS quantity       NUMERIC(12,2) NOT NULL DEFAULT 0;
ALTER TABLE orders    ADD COLUMN IF NOT EXISTS stock_deducted BOOLEAN       NOT NULL DEFAULT FALSE;
-- coffees.auto_sold_out đã có sẵn: app-core dùng lại để biết món do hệ thống tự đánh hết
COMMENT ON COLUMN coffees.auto_sold_out IS 'Hệ thống tự chuyển SOLD_OUT vì thiếu nguyên liệu; đủ lại thì tự mở bán';

COMMIT;
