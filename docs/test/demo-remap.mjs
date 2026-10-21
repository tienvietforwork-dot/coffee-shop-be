// Sinh SQL dời dữ liệu demo về đúng "ngày giả": node demo-remap.mjs demo-windows.json > demo-remap.sql
// Mọi mốc thời gian lưu dạng UTC (Hibernate jdbc.time_zone=UTC); giờ mở cửa 07:00–21:30 giờ VN = 00:00–14:30 UTC.
import fs from 'node:fs'

const windows = JSON.parse(fs.readFileSync(process.argv[2] ?? 'demo-windows.json', 'utf8'))
const nowUtc = new Date()
const iso = (d) => d.toISOString().replace('T', ' ').replace('Z', '')
const rows = windows.map((w) => {
  const fs0 = new Date(`${w.date}T00:00:00Z`)
  let fe = new Date(`${w.date}T14:30:00Z`)
  if (w.daysAgo === 0 && fe > new Date(nowUtc - 10 * 60e3)) fe = new Date(nowUtc - 10 * 60e3)
  return `('${iso(new Date(w.realStart))}'::timestamp, '${iso(new Date(w.realEnd))}'::timestamp, '${iso(fs0)}'::timestamp, '${iso(fe)}'::timestamp, '${w.date}'::date)`
})

// bảng của app-core có bản ghi demo; bảng của module khác (CRM, khuyến mãi, thống kê, log nền) không đụng tới
const TABLES = ['orders', 'order_items', 'order_status_history', 'payments', 'payment_transactions', 'shipments', 'shipment_events', 'invoices',
  'invoice_lines', 'notification_messages', 'incidents', 'vouchers', 'loyalty_point_history', 'customers', 'customer_addresses', 'carts', 'cart_items',
  'material_batches', 'material_transactions', 'material_packages', 'materials', 'coffees', 'purchase_orders', 'purchase_order_items',
  'goods_receipts', 'goods_receipt_items', 'supplier_returns', 'supplier_return_items', 'supplier_payments', 'suppliers', 'supplier_materials',
  'stocktakes', 'stocktake_items', 'cash_shifts', 'cash_movements', 'work_shifts', 'shift_assignments', 'staff', 'users', 'user_roles',
  'dining_tables', 'delivery_zones', 'coffee_cost_snapshots', 'audit_logs']
// giữ nguyên: ngày nhập tay đã là ngày giả
const KEEP = ['staff.hire_date', 'customers.date_of_birth', 'shift_assignments.work_date', 'shift_assignments.check_in_at', 'shift_assignments.check_out_at']
// tính từ "bây giờ" lúc tạo (hạn dùng, hạn link, hạn voucher): dời theo đúng độ lệch của created_at
const DERIVED = ['vouchers.expires_at', 'payment_transactions.expire_at', 'material_batches.expires_at']

console.log(`-- Dời dữ liệu demo về ${windows.length} ngày giả (${windows.at(-1)?.date} … ${windows[0]?.date}). Chạy 1 lần, trong 1 transaction.
BEGIN;
CREATE TEMP TABLE w (rs timestamp, re timestamp, fs timestamp, fe timestamp, d date) ON COMMIT DROP;
INSERT INTO w VALUES
${rows.join(',\n')};
CREATE FUNCTION pg_temp.remap(t timestamp) RETURNS timestamp LANGUAGE sql STABLE AS $$
  SELECT COALESCE((SELECT fs + (t - rs) * (extract(epoch FROM fe - fs) / NULLIF(extract(epoch FROM re - rs), 0)) FROM w WHERE t >= rs AND t < re), t)
$$;
CREATE FUNCTION pg_temp.inrun(t timestamp) RETURNS boolean LANGUAGE sql STABLE AS $$ SELECT EXISTS (SELECT 1 FROM w WHERE t >= rs AND t < re) $$;

-- mã chứng từ mang ngày thật: đổi sang ngày giả trước khi dời giờ (cần created_at cũ để biết khung)
CREATE TEMP TABLE oc ON COMMIT DROP AS
  SELECT o.id, o.order_code AS old_code, 'CH' || to_char(pg_temp.remap(o.created_at) + interval '7 hour', 'YYMMDD') || substr(o.order_code, 9) AS new_code
  FROM orders o WHERE pg_temp.inrun(o.created_at);
UPDATE orders o SET order_code = oc.new_code FROM oc WHERE oc.id = o.id;
UPDATE payment_transactions t SET txn_ref = replace(t.txn_ref, oc.old_code, oc.new_code) FROM payments p, oc WHERE p.id = t.payment_id AND p.order_id = oc.id;
UPDATE notification_messages n SET content = replace(n.content, oc.old_code, oc.new_code), request_id = replace(n.request_id, oc.old_code, oc.new_code) FROM oc WHERE n.order_id = oc.id;
DO $$
DECLARE r record; pfx text; width int;
BEGIN
  FOR r IN SELECT * FROM (VALUES ('purchase_orders','DH',3), ('goods_receipts','PN',3), ('supplier_returns','TH',3), ('supplier_payments','PC',3), ('stocktakes','KK',3), ('cash_shifts','CA',3)) v(t, p, n) LOOP
    EXECUTE format($q$
      WITH x AS (SELECT id, pg_temp.remap(created_at) + interval '7 hour' AS fake FROM %1$I WHERE pg_temp.inrun(created_at)),
           y AS (SELECT id, %2$L || to_char(fake, 'YYMMDD') || '-' || lpad((row_number() OVER (PARTITION BY fake::date ORDER BY fake, id) + 900)::text, %3$s, '0') AS c FROM x)
      UPDATE %1$I t SET code = y.c FROM y WHERE y.id = t.id$q$, r.t, r.p, r.n);
  END LOOP;
END $$;

-- dời mốc thời gian
DO $$
DECLARE c record;
BEGIN
  -- 1) cột ngày và cột "tính từ bây giờ": theo độ lệch của created_at của chính dòng đó
  FOR c IN SELECT table_name t, column_name col, data_type typ FROM information_schema.columns
           WHERE table_schema = 'public' AND table_name = ANY (ARRAY[${TABLES.map((t) => `'${t}'`).join(',')}])
             AND (data_type = 'date' OR table_name || '.' || column_name = ANY (ARRAY[${DERIVED.map((t) => `'${t}'`).join(',')}]))
             AND NOT table_name || '.' || column_name = ANY (ARRAY[${KEEP.map((t) => `'${t}'`).join(',')}]) LOOP
    IF c.typ = 'date' THEN
      EXECUTE format('UPDATE %I SET %I = %I + (pg_temp.remap(created_at)::date - created_at::date) WHERE pg_temp.inrun(created_at) AND %I IS NOT NULL', c.t, c.col, c.col, c.col);
    ELSE
      EXECUTE format('UPDATE %I SET %I = %I + (pg_temp.remap(created_at) - created_at) WHERE pg_temp.inrun(created_at) AND %I IS NOT NULL', c.t, c.col, c.col, c.col);
    END IF;
  END LOOP;
  -- 2) mọi cột timestamp còn lại (kể cả created_at, updated_at): giá trị nào rơi vào khung thì dời về ngày giả
  FOR c IN SELECT table_name t, column_name col FROM information_schema.columns
           WHERE table_schema = 'public' AND table_name = ANY (ARRAY[${TABLES.map((t) => `'${t}'`).join(',')}])
             AND data_type LIKE 'timestamp%'
             AND NOT table_name || '.' || column_name = ANY (ARRAY[${[...KEEP, ...DERIVED].map((t) => `'${t}'`).join(',')}])
           ORDER BY (column_name = 'created_at') LOOP
    EXECUTE format('UPDATE %I SET %I = pg_temp.remap(%I) WHERE pg_temp.inrun(%I)', c.t, c.col, c.col, c.col);
  END LOOP;
END $$;

-- tồn kho cuối ngày cho từng ngày giả: tồn hiện tại trừ ngược các giao dịch xảy ra sau ngày đó
INSERT INTO inventory_snapshots (snapshot_date, material_id, quantity, stock_value)
SELECT w.d, m.id,
       GREATEST(0, COALESCE(m.stock_quantity, 0) - COALESCE((SELECT sum(t.quantity) FROM material_transactions t JOIN material_batches b ON b.id = t.batch_id
                 WHERE b.material_id = m.id AND NOT t.del_flag AND t.created_at >= (w.d + 1)::timestamp - interval '7 hour'), 0)),
       0
FROM w CROSS JOIN materials m
WHERE NOT m.del_flag AND w.d < (now() + interval '7 hour')::date
ON CONFLICT (snapshot_date, material_id) DO NOTHING;
UPDATE inventory_snapshots s SET stock_value = round(s.quantity * COALESCE((SELECT sum(b.remaining_quantity * b.unit_cost) / NULLIF(sum(b.remaining_quantity), 0)
       FROM material_batches b WHERE b.material_id = s.material_id AND b.unit_cost IS NOT NULL AND b.remaining_quantity > 0),
       (SELECT b.unit_cost FROM material_batches b WHERE b.material_id = s.material_id AND b.unit_cost IS NOT NULL ORDER BY b.id DESC LIMIT 1), 0), 2)
WHERE s.snapshot_date IN (SELECT d FROM w) AND s.stock_value = 0;

-- chấm công các ngày đã qua (API không cho xếp ca vào ngày đã qua): mỗi người 1 ca cố định, nghỉ 1 ngày / tuần,
-- ~15% đi muộn 6–20 phút, ~4% nghỉ phép / không lương. Giờ lưu UTC = giờ VN − 7.
SELECT setseed(0.2026);
INSERT INTO shift_assignments (staff_id, work_shift_id, work_date, position, check_in_at, check_out_at, leave_type, note, created_by, created_at)
SELECT x.staff_id, x.shift_id, x.d, x.position,
       CASE WHEN x.r < 0.04 THEN NULL ELSE x.d + x.start_time + make_interval(mins => CASE WHEN x.r < 0.19 THEN 6 + floor(random() * 15)::int ELSE -floor(random() * 10)::int END) - interval '7 hour' END,
       CASE WHEN x.r < 0.04 THEN NULL ELSE x.d + x.end_time + make_interval(mins => floor(random() * 20)::int - 5) - interval '7 hour' END,
       CASE WHEN x.r < 0.025 THEN 'P' WHEN x.r < 0.04 THEN 'KL' END,
       CASE WHEN x.r < 0.04 THEN 'Xin nghỉ, đã báo quản lý' ELSE 'Máy chấm vân tay' END,
       'admin', x.d - 1 + time '02:00'
FROM (
  SELECT s.id AS staff_id, s.position, ws.id AS shift_id, ws.start_time, ws.end_time, w.d, random() AS r, s.rn
  FROM w
  CROSS JOIN (SELECT id, position, hire_date, row_number() OVER (ORDER BY id) AS rn FROM staff WHERE NOT del_flag AND work_status = 'ACTIVE') s
  JOIN work_shifts ws ON NOT ws.del_flag AND ws.name = (ARRAY['Ca sáng', 'Ca chiều', 'Ca tối'])[1 + (s.rn % 3)]
  WHERE w.d < (now() + interval '7 hour')::date AND (s.hire_date IS NULL OR s.hire_date <= w.d)
    AND extract(dow FROM w.d) <> (s.rn * 2) % 7
) x
WHERE NOT EXISTS (SELECT 1 FROM shift_assignments a WHERE a.staff_id = x.staff_id AND a.work_date = x.d AND a.work_shift_id = x.shift_id AND NOT a.del_flag);

COMMIT;`)
