-- Mã sản phẩm + chi tiết giảm giá trên đơn. Chạy được nhiều lần.
--  * coffees.code: mã sản phẩm (SP001…), duy nhất trong các món chưa xóa; món cũ lấy theo id.
--  * order_items.list_price: giá niêm yết lúc đặt (unit_price là giá đã giảm theo đợt giảm giá của món).
--    Dòng cũ: giá món hiện tại nếu đơn có đợt giảm giá và giá bán thấp hơn, ngược lại = unit_price.
--  * orders.voucher_code / voucher_discount: voucher đã dùng và số tiền voucher giảm
--    (discount_amount vẫn là tổng giảm của đơn = đợt giảm giá theo đơn + voucher).
-- Các cột đều cho phép NULL / có mặc định để bản app-core cũ (chưa map cột) vẫn tạo đơn, tạo món được.
BEGIN;

ALTER TABLE coffees ADD COLUMN IF NOT EXISTS code VARCHAR(20);
UPDATE coffees SET code = 'SP' || lpad(id::text, 3, '0') WHERE code IS NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uq_coffees_code ON coffees (upper(code)) WHERE NOT del_flag;

ALTER TABLE order_items ADD COLUMN IF NOT EXISTS list_price NUMERIC(12,2) CHECK (list_price >= 0);
UPDATE order_items i
SET list_price = CASE WHEN o.promotion_id IS NOT NULL AND c.price > i.unit_price THEN c.price ELSE i.unit_price END
FROM orders o, coffees c
WHERE o.id = i.order_id AND c.id = i.coffee_id AND i.list_price IS NULL;

ALTER TABLE orders ADD COLUMN IF NOT EXISTS voucher_code     VARCHAR(30);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS voucher_discount NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (voucher_discount >= 0);
UPDATE orders o
SET voucher_code = d.code, voucher_discount = LEAST(v.discount_value, o.discount_amount)
FROM voucher_details d JOIN vouchers v ON v.id = d.voucher_id
WHERE d.order_id = o.id AND o.voucher_code IS NULL;

COMMIT;
