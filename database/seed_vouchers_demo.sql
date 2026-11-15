-- Voucher mẫu để thử áp mã ở POS / trang đặt món: 3 loại giảm tiền cố định, mỗi loại 5 mã dùng chung
-- (customer_id NULL: ai nhập cũng được, mỗi mã dùng 1 lần), hạn 30 ngày kể từ lúc chạy. Chạy được nhiều lần.
BEGIN;

INSERT INTO vouchers (name, discount_type, discount_value, min_order_value, quantity, max_quantity, valid_days, status)
SELECT v.name, 'FIXED_AMOUNT', v.discount, v.min_order, 100, 100, 30, 'ACTIVE'
FROM (VALUES
  ('Chào bạn mới 10K',      10000::numeric,  50000::numeric),
  ('Giảm 20K đơn từ 100K',  20000::numeric, 100000::numeric),
  ('Giảm 30K đơn từ 150K',  30000::numeric, 150000::numeric)
) AS v(name, discount, min_order)
WHERE NOT EXISTS (SELECT 1 FROM vouchers x WHERE x.name = v.name AND NOT x.del_flag);

INSERT INTO voucher_details (voucher_id, code, customer_id, status, issued_at, expires_at)
SELECT t.id, p.prefix || lpad(n::text, 2, '0'), NULL, 'ISSUED', now(), now() + interval '30 day'
FROM (VALUES
  ('Chào bạn mới 10K',     'CF10K'),
  ('Giảm 20K đơn từ 100K', 'CF20K'),
  ('Giảm 30K đơn từ 150K', 'CF30K')
) AS p(name, prefix)
JOIN vouchers t ON t.name = p.name AND NOT t.del_flag
CROSS JOIN generate_series(1, 5) AS n
WHERE NOT EXISTS (SELECT 1 FROM voucher_details d WHERE upper(d.code) = p.prefix || lpad(n::text, 2, '0') AND NOT d.del_flag);

COMMIT;
