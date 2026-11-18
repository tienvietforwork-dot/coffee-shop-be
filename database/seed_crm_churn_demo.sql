-- Du lieu demo CRM: 5 khach de thu phan loai chi tieu va nguy co roi bo.
-- Chay tren database LOCAL/development. Khong chay tren database dung chung/production.
-- Co the chay lai: so dien thoai va order_code co dinh, INSERT trung se duoc bo qua.

BEGIN;

INSERT INTO customers (full_name, phone, email, description, created_by)
VALUES
    ('TEST CRM - VIP qua han 30 ngay',       '0999900101', 'crm.test.101@example.invalid', 'Du lieu demo CRM; co don thanh cong cu 31 ngay truoc.', 'crm-demo'),
    ('TEST CRM - VIP vua mua',               '0999900102', 'crm.test.102@example.invalid', 'Du lieu demo CRM; co don thanh cong 5 ngay truoc.', 'crm-demo'),
    ('TEST CRM - Tiem nang',                 '0999900103', 'crm.test.103@example.invalid', 'Du lieu demo CRM; chi tieu nam giua hai moc.', 'crm-demo'),
    ('TEST CRM - Thuong',                    '0999900104', 'crm.test.104@example.invalid', 'Du lieu demo CRM; chi tieu duoi moc tiem nang.', 'crm-demo'),
    ('TEST CRM - Chua tung mua',             '0999900105', 'crm.test.105@example.invalid', 'Du lieu demo CRM; khong co don thanh cong.', 'crm-demo')
ON CONFLICT (phone) WHERE NOT del_flag DO NOTHING;

INSERT INTO orders (order_code, customer_id, order_type, channel, status, subtotal, total_amount, ordered_at, created_by)
SELECT v.order_code, c.id, 'TAKE_AWAY', 'ONLINE', 'COMPLETED', v.amount, v.amount,
       CURRENT_TIMESTAMP - v.age, 'crm-demo'
FROM (VALUES
    ('CRM-DEMO-CHURN-101', '0999900101', 1200000::numeric, INTERVAL '31 days'),
    ('CRM-DEMO-VIP-102',   '0999900102',  700000::numeric, INTERVAL '5 days'),
    ('CRM-DEMO-POT-103',   '0999900103',  180000::numeric, INTERVAL '5 days'),
    ('CRM-DEMO-REG-104',   '0999900104',   20000::numeric, INTERVAL '5 days')
) AS v(order_code, phone, amount, age)
JOIN customers c ON c.phone = v.phone AND c.del_flag = FALSE
ON CONFLICT (order_code) DO NOTHING;

COMMIT;

-- Kiem tra 5 khach demo va don thanh cong gan nhat:
SELECT c.id, c.full_name, c.phone, COALESCE(SUM(o.total_amount), 0) AS total_spend,
       MAX(o.ordered_at) AS last_completed_order
FROM customers c
LEFT JOIN orders o ON o.customer_id = c.id AND o.del_flag = FALSE AND o.status = 'COMPLETED'
WHERE c.phone BETWEEN '0999900101' AND '0999900105'
  AND c.del_flag = FALSE
GROUP BY c.id, c.full_name, c.phone
ORDER BY c.phone;
