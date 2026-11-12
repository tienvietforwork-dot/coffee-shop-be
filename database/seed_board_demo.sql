-- Dữ liệu mẫu cho bảng "Điều phối đơn hàng": đủ 6 cột (Chờ xác nhận, Hẹn giao, Chờ pha chế, Đang pha chế, Sẵn sàng,
-- Đang giao) + 3 gói giao định kỳ. Mọi giờ tính theo lúc chạy (giờ Việt Nam). Chạy lại được: đơn / gói mẫu cũ còn đang
-- xử lý bị ẩn (del_flag) rồi tạo bộ mới. Mã đơn mẫu: DEMO-<lần chạy>-XXXX (bảng hiện 4 ký tự cuối).
-- Gỡ hết: UPDATE orders SET del_flag = true WHERE order_code LIKE 'DEMO-%';
--         UPDATE delivery_subscriptions SET del_flag = true WHERE code LIKE 'GDDEMO%';
BEGIN;

UPDATE orders SET del_flag = true, del_user = 'demo-seed'
WHERE order_code LIKE 'DEMO-%' AND status IN ('PENDING', 'CONFIRMED', 'PREPARING', 'READY', 'DELIVERING') AND NOT del_flag;
UPDATE delivery_subscriptions SET del_flag = true, del_user = 'demo-seed' WHERE code LIKE 'GDDEMO%' AND NOT del_flag;

-- một món trong đơn mẫu (giá theo thực đơn hiện tại)
CREATE OR REPLACE FUNCTION pg_temp.demo_item(p_order BIGINT, p_code TEXT, p_qty INT, p_note TEXT DEFAULT NULL) RETURNS NUMERIC AS $$
DECLARE c RECORD;
BEGIN
  SELECT id, price INTO c FROM coffees WHERE code = p_code AND NOT del_flag;
  INSERT INTO order_items (order_id, coffee_id, quantity, list_price, unit_price, line_total, note)
  VALUES (p_order, c.id, p_qty, c.price, c.price, c.price * p_qty, p_note);
  RETURN c.price * p_qty;
END $$ LANGUAGE plpgsql;

-- một đơn mẫu: tổng tiền = các món + phí ship; thanh toán tiền mặt khi nhận (chưa TT) trừ khi p_paid
CREATE OR REPLACE FUNCTION pg_temp.demo_order(p_code TEXT, p_status TEXT, p_type TEXT, p_channel TEXT, p_customer BIGINT,
                                   p_address BIGINT, p_ordered TIMESTAMP, p_pickup TIMESTAMP, p_items TEXT[],
                                   p_note TEXT DEFAULT NULL, p_paid BOOLEAN DEFAULT FALSE, p_sub BIGINT DEFAULT NULL)
RETURNS BIGINT AS $$
DECLARE oid BIGINT; total NUMERIC := 0; fee NUMERIC := CASE WHEN p_type = 'DELIVERY' THEN 15000 ELSE 0 END; it TEXT;
BEGIN
  -- tham số là giờ Việt Nam; cột TIMESTAMP lưu UTC (app-core: hibernate.jdbc.time_zone = UTC)
  p_ordered := (p_ordered AT TIME ZONE 'Asia/Ho_Chi_Minh') AT TIME ZONE 'UTC';
  p_pickup  := (p_pickup  AT TIME ZONE 'Asia/Ho_Chi_Minh') AT TIME ZONE 'UTC';
  INSERT INTO orders (order_code, customer_id, order_type, channel, status, note, pickup_time, ordered_at, subscription_id,
                      stock_deducted, created_by, created_at)
  VALUES (p_code, p_customer, p_type, p_channel, p_status, p_note, p_pickup, p_ordered, p_sub,
          p_status IN ('PREPARING', 'READY', 'DELIVERING'), 'demo-seed', p_ordered)
  RETURNING id INTO oid;
  FOREACH it IN ARRAY p_items LOOP -- 'SP001x2'
    total := total + pg_temp.demo_item(oid, split_part(it, 'x', 1), split_part(it, 'x', 2)::INT);
  END LOOP;
  UPDATE orders SET subtotal = total, total_amount = total + fee WHERE id = oid;
  INSERT INTO payments (order_id, method, amount, status, paid_at, created_by)
  VALUES (oid, 'CASH', total + fee, CASE WHEN p_paid THEN 'PAID' ELSE 'PENDING' END, CASE WHEN p_paid THEN p_ordered END, 'demo-seed');
  IF p_type = 'DELIVERY' THEN
    INSERT INTO shipments (order_id, address_id, shipping_fee, status, zone_name, picked_up_at, created_by)
    VALUES (oid, p_address, fee, CASE WHEN p_status = 'DELIVERING' THEN 'DELIVERING' ELSE 'PENDING' END,
            'Thành phố Hà Nội', CASE WHEN p_status = 'DELIVERING' THEN p_ordered + interval '15 minute' END, 'demo-seed');
  END IF;
  INSERT INTO order_status_history (order_id, from_status, to_status, note, created_by, created_at)
  VALUES (oid, NULL, p_status, 'Dữ liệu mẫu', 'demo-seed', p_ordered);
  RETURN oid;
END $$ LANGUAGE plpgsql;

-- giờ hẹn giao thật: làm tròn lên mốc :00 / :30, trong khung [mở cửa + 1h, đóng cửa − 30') để quán kịp pha và giao;
-- quá khung thì dời sang sáng hôm sau
CREATE OR REPLACE FUNCTION pg_temp.slot(p_at TIMESTAMP, p_open TIME, p_close TIME) RETURNS TIMESTAMP AS $$
DECLARE t TIMESTAMP := date_trunc('hour', p_at) + CASE WHEN extract(minute FROM p_at) = 0 THEN interval '0'
                         WHEN extract(minute FROM p_at) <= 30 THEN interval '30 minute' ELSE interval '1 hour' END;
BEGIN
  IF t::time > p_close - interval '30 minute' THEN t := t::date + 1 + p_open + interval '1 hour'; END IF;
  IF t::time < p_open + interval '1 hour' THEN t := t::date + p_open + interval '1 hour'; END IF;
  RETURN t;
END $$ LANGUAGE plpgsql;

DO $$
DECLARE
  now_vn TIMESTAMP := date_trunc('minute', now() AT TIME ZONE 'Asia/Ho_Chi_Minh');
  run    TEXT := to_char(now() AT TIME ZONE 'Asia/Ho_Chi_Minh', 'DDHH24MISS');
  o TIME; cl TIME; sub_at TIMESTAMP;
  c1 BIGINT; c2 BIGINT; c3 BIGINT; a1 BIGINT; a2 BIGINT; a3 BIGINT; sub1 BIGINT;
BEGIN
  -- giờ mở cửa của quán (mặc định 08:00–22:00); đơn mẫu đặt trong giờ mở cửa, trước bây giờ
  SELECT open_time::time, close_time::time INTO o, cl FROM delivery_settings WHERE NOT del_flag ORDER BY id LIMIT 1;
  o := coalesce(o, time '08:00'); cl := coalesce(cl, time '22:00');
  IF now_vn::time < o + interval '1 hour' OR now_vn::time > cl - interval '45 minute' THEN
    RAISE EXCEPTION 'Chạy dữ liệu mẫu trong giờ mở cửa (% – %), từ % tới %', o, cl, o + interval '1 hour', cl - interval '45 minute';
  END IF;
  sub_at := pg_temp.slot(now_vn + interval '2 hour 30 minute', o, cl);
  -- khách mẫu (tạo nếu chưa có) + một địa chỉ giao mẫu cho mỗi khách
  SELECT id INTO c1 FROM customers WHERE phone = '0912345678' AND NOT del_flag ORDER BY id LIMIT 1;
  IF c1 IS NULL THEN INSERT INTO customers (full_name, phone) VALUES ('Đặng Hoài Nam', '0912345678') RETURNING id INTO c1; END IF;
  SELECT id INTO c2 FROM customers WHERE phone = '0987654321' AND NOT del_flag ORDER BY id LIMIT 1;
  IF c2 IS NULL THEN INSERT INTO customers (full_name, phone) VALUES ('Bùi Ngọc Lan', '0987654321') RETURNING id INTO c2; END IF;
  SELECT id INTO c3 FROM customers WHERE phone = '0908447799' AND NOT del_flag ORDER BY id LIMIT 1;
  IF c3 IS NULL THEN INSERT INTO customers (full_name, phone) VALUES ('Kiều Văn Lộc', '0908447799') RETURNING id INTO c3; END IF;

  SELECT id INTO a1 FROM customer_addresses WHERE customer_id = c1 AND label = 'Demo' AND NOT del_flag LIMIT 1;
  IF a1 IS NULL THEN
    INSERT INTO customer_addresses (customer_id, label, recipient_name, recipient_phone, address)
    VALUES (c1, 'Demo', 'Đặng Hoài Nam', '0912345678', '12 Phố Láng Hạ, Phường Láng, Thành phố Hà Nội') RETURNING id INTO a1;
  END IF;
  SELECT id INTO a2 FROM customer_addresses WHERE customer_id = c2 AND label = 'Demo' AND NOT del_flag LIMIT 1;
  IF a2 IS NULL THEN
    INSERT INTO customer_addresses (customer_id, label, recipient_name, recipient_phone, address)
    VALUES (c2, 'Demo', 'Bùi Ngọc Lan', '0987654321', 'Tòa nhà Vinaconex, 34 Láng Hạ, Phường Đống Đa, Thành phố Hà Nội') RETURNING id INTO a2;
  END IF;
  SELECT id INTO a3 FROM customer_addresses WHERE customer_id = c3 AND label = 'Demo' AND NOT del_flag LIMIT 1;
  IF a3 IS NULL THEN
    INSERT INTO customer_addresses (customer_id, label, recipient_name, recipient_phone, address)
    VALUES (c3, 'Demo', 'Kiều Văn Lộc', '0908447799', '5 Phố Nguyễn Chí Thanh, Phường Láng, Thành phố Hà Nội') RETURNING id INTO a3;
  END IF;

  -- gói giao định kỳ: GDDEMO1 đã tạo đơn cho lần giao gần nhất (đơn HG04), GDDEMO2 mọi thứ trong tuần trước giờ đóng
  -- cửa 30', GDDEMO3 mỗi sáng từ mai lúc mở cửa + 1h
  INSERT INTO delivery_subscriptions (code, customer_id, address_id, frequency, days, delivery_time, start_date, end_date,
                                      status, note, last_run_date, created_by)
  VALUES ('GDDEMO1', c1, a1, 'DAILY', NULL, to_char(sub_at, 'HH24:MI'), now_vn::date - 7, now_vn::date + 7,
          'ACTIVE', 'Giao tới lễ tân', sub_at::date, 'demo-seed')
  RETURNING id INTO sub1;
  INSERT INTO delivery_subscription_items (subscription_id, coffee_id, quantity)
  SELECT sub1, id, q FROM (VALUES ('SP001', 2), ('SP006', 1)) v(code, q) JOIN coffees c ON c.code = v.code AND NOT c.del_flag;

  WITH s AS (
    INSERT INTO delivery_subscriptions (code, customer_id, address_id, frequency, days, delivery_time, start_date, end_date, status, note, created_by)
    VALUES ('GDDEMO2', c2, a2, 'WEEKLY', '1,2,3,4,5,6,7', to_char(cl - interval '30 minute', 'HH24:MI'), now_vn::date, now_vn::date + 7, 'ACTIVE', 'Văn phòng tăng ca', 'demo-seed')
    RETURNING id)
  INSERT INTO delivery_subscription_items (subscription_id, coffee_id, quantity)
  SELECT s.id, c.id, v.q FROM s, (VALUES ('SP004', 3), ('SP008', 2)) v(code, q) JOIN coffees c ON c.code = v.code AND NOT c.del_flag;

  WITH s AS (
    INSERT INTO delivery_subscriptions (code, customer_id, address_id, frequency, days, delivery_time, start_date, end_date, status, note, created_by)
    VALUES ('GDDEMO3', c3, a3, 'DAILY', NULL, to_char(o + interval '1 hour', 'HH24:MI'), now_vn::date + 1, now_vn::date + 7, 'ACTIVE', 'Cà phê sáng cho phòng họp', 'demo-seed')
    RETURNING id)
  INSERT INTO delivery_subscription_items (subscription_id, coffee_id, quantity)
  SELECT s.id, c.id, v.q FROM s, (VALUES ('SP002', 4), ('SP003', 2)) v(code, q) JOIN coffees c ON c.code = v.code AND NOT c.del_flag;

  -- Chờ xác nhận
  PERFORM pg_temp.demo_order('DEMO-' || run || '-CX01', 'PENDING', 'DELIVERY', 'ONLINE', c1, a1, now_vn - interval '3 minute', NULL, ARRAY['SP001x1', 'SP006x2']);
  PERFORM pg_temp.demo_order('DEMO-' || run || '-CX02', 'PENDING', 'DELIVERY', 'ONLINE', c2, a2, now_vn - interval '1 minute',
                             pg_temp.slot(now_vn + interval '4 hour 30 minute', o, cl), ARRAY['SP004x2'], 'Gọi trước khi tới');
  -- Hẹn giao (chưa tới giờ phải pha)
  PERFORM pg_temp.demo_order('DEMO-' || run || '-HG01', 'CONFIRMED', 'DELIVERY', 'ONLINE', c3, a3, now_vn - interval '40 minute',
                             pg_temp.slot(now_vn + interval '1 hour 45 minute', o, cl), ARRAY['SP008x1', 'SP012x2']);
  PERFORM pg_temp.demo_order('DEMO-' || run || '-HG02', 'CONFIRMED', 'TAKE_AWAY', 'ONLINE', c2, NULL, now_vn - interval '25 minute',
                             pg_temp.slot(now_vn + interval '3 hour 15 minute', o, cl), ARRAY['SP005x2', 'SP007x1'], NULL, TRUE);
  PERFORM pg_temp.demo_order('DEMO-' || run || '-HG03', 'CONFIRMED', 'DELIVERY', 'ONLINE', c1, a1, now_vn - interval '15 minute',
                             now_vn::date + 1 + o + interval '1 hour 30 minute', ARRAY['SP010x3'], 'Giao sáng mai');
  PERFORM pg_temp.demo_order('DEMO-' || run || '-HG04', 'CONFIRMED', 'DELIVERY', 'ONLINE', c1, a1, now_vn - interval '5 minute', sub_at,
                             ARRAY['SP001x2', 'SP006x1'], 'Gói định kỳ GDDEMO1 · Giao tới lễ tân', FALSE, sub1);
  -- Chờ pha chế: đơn hẹn giờ đã tới giờ pha (xếp đầu) + đơn làm ngay
  PERFORM pg_temp.demo_order('DEMO-' || run || '-CP01', 'CONFIRMED', 'DELIVERY', 'ONLINE', c2, a2, now_vn - interval '50 minute',
                             now_vn + interval '30 minute', ARRAY['SP011x2']);
  PERFORM pg_temp.demo_order('DEMO-' || run || '-CP02', 'CONFIRMED', 'TAKE_AWAY', 'ONLINE', c3, NULL, now_vn - interval '8 minute', NULL, ARRAY['SP002x1', 'SP009x1'], NULL, TRUE);
  PERFORM pg_temp.demo_order('DEMO-' || run || '-CP03', 'CONFIRMED', 'DELIVERY', 'ONLINE', c1, a1, now_vn - interval '4 minute', NULL, ARRAY['SP003x2']);
  -- Đang pha chế / Sẵn sàng / Đang giao
  PERFORM pg_temp.demo_order('DEMO-' || run || '-PC01', 'PREPARING', 'TAKE_AWAY', 'ONLINE', c2, NULL, now_vn - interval '12 minute', NULL, ARRAY['SP004x1'], NULL, TRUE);
  PERFORM pg_temp.demo_order('DEMO-' || run || '-SS01', 'READY', 'DELIVERY', 'ONLINE', c3, a3, now_vn - interval '20 minute', NULL, ARRAY['SP006x2']);
  PERFORM pg_temp.demo_order('DEMO-' || run || '-DG01', 'DELIVERING', 'DELIVERY', 'ONLINE', c1, a1, now_vn - interval '35 minute', NULL, ARRAY['SP001x1', 'SP012x1']);
END $$;

COMMIT;
