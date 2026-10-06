-- =====================================================================
-- Coffeeholic – dữ liệu khởi tạo (chạy sau schema.sql, trên DB trống).
--  Phần 1: phân quyền (3 role, danh mục quyền, quyền mặc định) – BẮT BUỘC.
--  Phần 2: dữ liệu demo (thực đơn, kho, bàn, nhân viên, tài khoản, khuyến mãi, voucher).
-- Mật khẩu mọi tài khoản demo: admin123  (đổi ngay khi triển khai thật).
-- Ảnh dùng Unsplash (hotlink được).
-- =====================================================================

BEGIN;

-- ======================= Phần 1: phân quyền =========================
INSERT INTO roles (code, name, description) VALUES
 ('ADMIN',    'Quản lý',    'Toàn quyền hệ thống quản trị'),
 ('STAFF',    'Nhân viên',  'Bán hàng, pha chế, giao nhận, kho, chăm sóc khách'),
 ('CUSTOMER', 'Khách hàng', 'Tài khoản khách: đơn hàng, điểm tích lũy, voucher của mình');

-- Mỗi màn hình 1 quyền (như MENU_CD / MENU_PATH của MES); path NULL = quyền thao tác.
-- Mã quyền phải khớp com.coffeeshop.security.Perm (backend) và src/lib/perm.ts (frontend).
INSERT INTO permissions (code, name, path, module, description, sort_order) VALUES
 ('dashboard',            'Tổng quan',                    '/admin',                      'core',       NULL, 10),
 ('orders',               'Điều phối đơn hàng',            '/admin/orders',               'core',       'Xác nhận, từ chối, hủy, pha chế, hoàn tất, thu tiền', 20),
 ('orders.refund',        'Hoàn tiền',                     NULL,                          'core',       'Hoàn tiền khoản đã thanh toán', 21),
 ('pos',                  'Order tại quầy',                '/admin/pos',                  'core',       NULL, 30),
 ('shipments',            'Giao hàng',                     '/admin/shipments',            'core',       'Đặt xe, cập nhật trạng thái giao', 40),
 ('incidents',            'Sự cố đơn hàng',                '/admin/incidents',            'core',       'Xem và ghi nhận sự cố', 50),
 ('incidents.resolve',    'Xử lý sự cố & đền bù voucher',  NULL,                          'core',       NULL, 51),
 ('menu',                 'Cà phê & công thức',            '/admin/coffees',              'core',       'Xem thực đơn, công thức; bật/tắt bán, còn/hết món', 60),
 ('menu.edit',            'Sửa thực đơn & công thức',      NULL,                          'core',       'Tạo/sửa cà phê, giá, danh mục, công thức', 61),
 ('categories',           'Danh mục',                      '/admin/categories',           'core',       NULL, 62),
 ('materials',            'Nguyên liệu & lô',              '/admin/materials',            'core',       NULL, 70),
 ('materials.edit',       'Sửa nguyên liệu & kiểm kê',     NULL,                          'core',       'Tạo/sửa nguyên liệu, mức tồn tối thiểu, kiểm kê', 71),
 ('stock',                'Nhập / xuất kho',               '/admin/stock',                'core',       NULL, 72),
 ('stock-history',        'Lịch sử kho',                   '/admin/stock-history',        'core',       NULL, 73),
 ('tables',               'Bàn & mã QR',                   '/admin/tables',               'core',       NULL, 80),
 ('tables.edit',          'Sửa bàn & tạo lại QR',          NULL,                          'core',       NULL, 81),
 ('crm.customers',        'Hồ sơ khách hàng',              '/admin/crm/customers',        'crm',        NULL, 100),
 ('crm.interactions',     'Tương tác & chăm sóc',          '/admin/crm/interactions',     'crm',        NULL, 101),
 ('crm.feedbacks',        'Phản hồi & khiếu nại',          '/admin/crm/feedbacks',        'crm',        NULL, 102),
 ('crm.groups',           'Nhóm khách hàng',               '/admin/crm/groups',           'crm',        NULL, 103),
 ('crm.insights',         'Phân tích khách hàng',          '/admin/crm/insights',         'crm',        NULL, 104),
 ('promotions',           'Đợt giảm giá',                  '/admin/promotions',           'promotions', NULL, 200),
 ('vouchers',             'Voucher',                       '/admin/vouchers',             'promotions', NULL, 201),
 ('promotions.analytics', 'Hiệu quả khuyến mãi',           '/admin/promotions/analytics', 'promotions', NULL, 202),
 ('stats',                'Thống kê doanh thu',            '/admin/stats',                'stats',      NULL, 300),
 ('staff',                'Nhân viên',                     '/admin/staff',                'system',     NULL, 400),
 ('users',                'Tài khoản',                     '/admin/users',                'system',     NULL, 401),
 ('roles',                'Phân quyền',                    '/admin/roles',                'system',     'Gán quyền cho 3 role', 402),
 ('account',              'Tài khoản của tôi',             '/account',                    'customer',   'Đơn hàng, điểm, voucher của khách', 500);

-- Quản lý: toàn bộ quyền quản trị
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p WHERE r.code = 'ADMIN' AND p.module <> 'customer';

-- Nhân viên: vận hành hằng ngày
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON p.code IN (
    'dashboard', 'orders', 'pos', 'shipments', 'incidents', 'menu', 'categories', 'materials', 'stock',
    'stock-history', 'tables', 'crm.customers', 'crm.interactions', 'crm.feedbacks')
WHERE r.code = 'STAFF';

-- Khách hàng
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON p.code = 'account' WHERE r.code = 'CUSTOMER';

-- ======================= Phần 2: dữ liệu demo =======================
INSERT INTO staff (full_name, position, phone, email, hire_date) VALUES
 ('Nguyễn Minh Anh', 'Quản lý cửa hàng', '0901000001', 'anh.nm@coffeeholic.vn', '2024-01-15'),
 ('Trần Quốc Bảo',   'Barista',          '0901000002', 'bao.tq@coffeeholic.vn', '2024-03-01'),
 ('Lê Thu Hà',       'Barista',          '0901000003', 'ha.lt@coffeeholic.vn',  '2024-06-10'),
 ('Phạm Gia Huy',    'Thu ngân',         '0901000004', 'huy.pg@coffeeholic.vn', '2025-02-20'),
 ('Võ Thanh Tâm',    'Phục vụ / giao hàng', '0901000005', NULL,                 '2025-05-05');

INSERT INTO customers (full_name, phone, email, loyalty_points) VALUES
 ('Đặng Hoài Nam', '0912345678', 'nam.dh@gmail.com', 42),
 ('Bùi Ngọc Lan',  '0987654321', NULL, 15);

-- Tài khoản: admin (Quản lý), staff1/staff2 (Nhân viên), 0912345678 (Khách hàng)
INSERT INTO users (username, password_hash, full_name, email, phone, staff_id, customer_id) VALUES
 ('admin',      '$2b$10$adu/nwAFovsEvFlnSXPpKuFE.8MW4roZoET1hYF3zBOR.cBNxMoAW', 'Nguyễn Minh Anh', 'anh.nm@coffeeholic.vn', '0901000001',
  (SELECT id FROM staff WHERE phone = '0901000001'), NULL),
 ('staff1',     '$2b$10$adu/nwAFovsEvFlnSXPpKuFE.8MW4roZoET1hYF3zBOR.cBNxMoAW', 'Trần Quốc Bảo',   'bao.tq@coffeeholic.vn', '0901000002',
  (SELECT id FROM staff WHERE phone = '0901000002'), NULL),
 ('staff2',     '$2b$10$adu/nwAFovsEvFlnSXPpKuFE.8MW4roZoET1hYF3zBOR.cBNxMoAW', 'Phạm Gia Huy',    'huy.pg@coffeeholic.vn', '0901000004',
  (SELECT id FROM staff WHERE phone = '0901000004'), NULL),
 ('0912345678', '$2b$10$adu/nwAFovsEvFlnSXPpKuFE.8MW4roZoET1hYF3zBOR.cBNxMoAW', 'Đặng Hoài Nam',   'nam.dh@gmail.com',      '0912345678',
  NULL, (SELECT id FROM customers WHERE phone = '0912345678'));

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u JOIN roles r ON r.code = CASE
    WHEN u.username = 'admin' THEN 'ADMIN'
    WHEN u.customer_id IS NOT NULL THEN 'CUSTOMER'
    ELSE 'STAFF' END;

INSERT INTO categories (name, description, display_order) VALUES
 ('Cà phê Việt',     'Phin truyền thống, đậm vị Robusta Buôn Ma Thuột', 1),
 ('Espresso',        'Pha máy từ hạt Arabica rang vừa',                 2),
 ('Cold Brew',       'Ủ lạnh 18 giờ, êm và ít đắng',                     3),
 ('Cà phê đặc biệt', 'Công thức riêng của Coffeeholic',                 4);

INSERT INTO coffees (category_id, name, image_url, price, description, status)
SELECT c.id, v.name, v.img, v.price, v.descr, v.status FROM (VALUES
 ('Cà phê Việt', 'Cà phê đen đá', 'https://images.unsplash.com/photo-1514432324607-a09d9b4aefdd?w=800&q=80', 29000, 'Phin Robusta nguyên chất, đậm và thơm', 'AVAILABLE'),
 ('Cà phê Việt', 'Cà phê sữa đá', 'https://images.unsplash.com/photo-1461023058943-07fcbe16d735?w=800&q=80', 35000, 'Phin Robusta cùng sữa đặc, ly cà phê quen thuộc', 'AVAILABLE'),
 ('Cà phê Việt', 'Bạc xỉu', 'https://images.unsplash.com/photo-1572442388796-11668a67e53d?w=800&q=80', 39000, 'Nhiều sữa, ít cà phê, ngọt dịu', 'AVAILABLE'),
 ('Espresso', 'Espresso', 'https://images.unsplash.com/photo-1510591509098-f4fdc6d0ff04?w=800&q=80', 35000, 'Shot espresso Arabica Cầu Đất', 'AVAILABLE'),
 ('Espresso', 'Americano', 'https://images.unsplash.com/photo-1551030173-122aabc4489c?w=800&q=80', 39000, 'Espresso pha loãng, thanh và nhẹ', 'AVAILABLE'),
 ('Espresso', 'Cappuccino', 'https://images.unsplash.com/photo-1572442388796-11668a67e53d?w=800&q=80', 49000, 'Espresso, sữa nóng và lớp bọt mịn', 'AVAILABLE'),
 ('Espresso', 'Latte', 'https://images.unsplash.com/photo-1541167760496-1628856ab772?w=800&q=80', 52000, 'Espresso với nhiều sữa tươi đánh nóng', 'AVAILABLE'),
 ('Cold Brew', 'Cold Brew truyền thống', 'https://images.unsplash.com/photo-1517701604599-bb29b565090c?w=800&q=80', 45000, 'Ủ lạnh 18 giờ, hậu vị chocolate', 'AVAILABLE'),
 ('Cold Brew', 'Cold Brew cam sả', 'https://images.unsplash.com/photo-1499961024600-ad094db305cc?w=800&q=80', 55000, 'Cold brew cùng cam vàng và sả tươi', 'AVAILABLE'),
 ('Cà phê đặc biệt', 'Cà phê muối', 'https://images.unsplash.com/photo-1485808191679-5f86510681a2?w=800&q=80', 45000, 'Kem muối Huế béo mặn trên nền cà phê phin', 'AVAILABLE'),
 ('Cà phê đặc biệt', 'Cà phê trứng', 'https://images.unsplash.com/photo-1534778101976-62847782c213?w=800&q=80', 49000, 'Kem trứng đánh bông kiểu Hà Nội', 'AVAILABLE'),
 ('Cà phê đặc biệt', 'Cà phê dừa', 'https://images.unsplash.com/photo-1525803377221-4f6ccdaa5133?w=800&q=80', 52000, 'Cốt dừa đá xay và cà phê phin', 'SOLD_OUT')
) AS v(cat, name, img, price, descr, status) JOIN categories c ON c.name = v.cat;

INSERT INTO materials (name, unit, min_stock) VALUES
 ('Cà phê Robusta rang xay', 'g', 2000), ('Hạt Arabica', 'g', 1500), ('Sữa đặc', 'ml', 2000),
 ('Sữa tươi', 'ml', 3000), ('Đường', 'g', 1000), ('Kem muối', 'ml', 500), ('Trứng gà', 'quả', 20),
 ('Cốt dừa', 'ml', 1000), ('Cam vàng', 'quả', 10), ('Đá viên', 'g', 5000);

INSERT INTO material_batches (material_id, import_quantity, remaining_quantity, unit_cost, expiry_date)
SELECT m.id, v.q, v.q, v.c, CURRENT_DATE + v.d FROM (VALUES
 ('Cà phê Robusta rang xay', 8000, 0.25, 60), ('Hạt Arabica', 5000, 0.45, 90), ('Sữa đặc', 6000, 0.06, 120),
 ('Sữa tươi', 8000, 0.035, 5), ('Đường', 5000, 0.025, 365), ('Kem muối', 1500, 0.12, 4), ('Trứng gà', 60, 3500, 10),
 ('Cốt dừa', 800, 0.09, 30), ('Cam vàng', 30, 9000, 7), ('Đá viên', 30000, 0.002, 2)
) AS v(name, q, c, d) JOIN materials m ON m.name = v.name;

UPDATE materials m SET stock_quantity = b.total
FROM (SELECT material_id, SUM(remaining_quantity) total FROM material_batches GROUP BY material_id) b
WHERE b.material_id = m.id;

INSERT INTO material_transactions (batch_id, staff_id, type, quantity, note)
SELECT b.id, (SELECT id FROM staff WHERE phone = '0901000001'), 'IMPORT', b.import_quantity, 'Nhập kho ban đầu'
FROM material_batches b;

INSERT INTO recipes (coffee_id, brew_method, version, brew_time_min, is_active)
SELECT c.id, v.bm, 1, v.t, TRUE FROM (VALUES
 ('Cà phê đen đá','PHIN',5), ('Cà phê sữa đá','PHIN',5), ('Bạc xỉu','PHIN',5),
 ('Espresso','MACHINE',2), ('Americano','MACHINE',3), ('Cappuccino','MACHINE',4), ('Latte','MACHINE',4),
 ('Cold Brew truyền thống','COLD_BREW',2), ('Cold Brew cam sả','COLD_BREW',3),
 ('Cà phê muối','PHIN',6), ('Cà phê trứng','PHIN',8), ('Cà phê dừa','PHIN',6)
) AS v(name, bm, t) JOIN coffees c ON c.name = v.name;

INSERT INTO recipe_materials (recipe_id, material_id, quantity)
SELECT r.id, m.id, v.q FROM (VALUES
 ('Cà phê đen đá','Cà phê Robusta rang xay',25), ('Cà phê đen đá','Đường',10), ('Cà phê đen đá','Đá viên',150),
 ('Cà phê sữa đá','Cà phê Robusta rang xay',25), ('Cà phê sữa đá','Sữa đặc',30), ('Cà phê sữa đá','Đá viên',150),
 ('Bạc xỉu','Cà phê Robusta rang xay',12), ('Bạc xỉu','Sữa đặc',30), ('Bạc xỉu','Sữa tươi',60), ('Bạc xỉu','Đá viên',150),
 ('Espresso','Hạt Arabica',18),
 ('Americano','Hạt Arabica',18), ('Americano','Đá viên',100),
 ('Cappuccino','Hạt Arabica',18), ('Cappuccino','Sữa tươi',120),
 ('Latte','Hạt Arabica',18), ('Latte','Sữa tươi',180),
 ('Cold Brew truyền thống','Hạt Arabica',30), ('Cold Brew truyền thống','Đá viên',150),
 ('Cold Brew cam sả','Hạt Arabica',30), ('Cold Brew cam sả','Cam vàng',0.5), ('Cold Brew cam sả','Đá viên',150),
 ('Cà phê muối','Cà phê Robusta rang xay',25), ('Cà phê muối','Kem muối',40), ('Cà phê muối','Sữa đặc',20),
 ('Cà phê trứng','Cà phê Robusta rang xay',20), ('Cà phê trứng','Trứng gà',1), ('Cà phê trứng','Sữa đặc',25),
 ('Cà phê dừa','Cà phê Robusta rang xay',20), ('Cà phê dừa','Cốt dừa',60), ('Cà phê dừa','Đá viên',200)
) AS v(coffee, material, q)
JOIN coffees c ON c.name = v.coffee
JOIN recipes r ON r.coffee_id = c.id AND r.is_active
JOIN materials m ON m.name = v.material;

INSERT INTO recipe_steps (recipe_id, step_no, instruction)
SELECT r.id, v.n, v.s FROM (VALUES
 ('Cà phê sữa đá',1,'Tráng phin bằng nước sôi, cho 25g cà phê, nén nhẹ'),
 ('Cà phê sữa đá',2,'Rót 20ml nước sôi ủ 30 giây, sau đó rót thêm 60ml'),
 ('Cà phê sữa đá',3,'Cho 30ml sữa đặc vào ly, đổ cà phê, khuấy đều và thêm đá'),
 ('Latte',1,'Chiết xuất 1 shot espresso 18g / 36ml trong 28 giây'),
 ('Latte',2,'Đánh nóng 180ml sữa tươi tới 60°C'),
 ('Latte',3,'Rót sữa vào espresso, tạo latte art'),
 ('Cà phê muối',1,'Pha phin 25g cà phê với 60ml nước'),
 ('Cà phê muối',2,'Cho sữa đặc và đá vào ly, rót cà phê'),
 ('Cà phê muối',3,'Phủ 40ml kem muối lên trên, không khuấy')
) AS v(coffee, n, s)
JOIN coffees c ON c.name = v.coffee JOIN recipes r ON r.coffee_id = c.id AND r.is_active;

INSERT INTO dining_tables (table_no, qr_code, area, capacity) VALUES
 ('B01','tbl-b01-7f3k2m','Tầng trệt',2), ('B02','tbl-b02-9q4x1p','Tầng trệt',4),
 ('B03','tbl-b03-2h8n5r','Tầng trệt',4), ('B04','tbl-b04-6w1c7v','Sân vườn',6),
 ('B05','tbl-b05-3j9t4y','Sân vườn',4), ('B06','tbl-b06-8d2z6s','Lầu 1',2);

INSERT INTO promotions (name, description, apply_scope, discount_percent, min_order_amount, start_date, end_date, status) VALUES
 ('Giảm 10% đơn từ 150K', 'Áp dụng cho mọi đơn từ 150.000đ', 'ORDER', 10, 150000,
  date_trunc('day', now()) - interval '1 day', date_trunc('day', now()) + interval '60 day', 'ACTIVE'),
 ('Giờ vàng Cold Brew', 'Cold Brew giảm giá, số lượng có hạn', 'COFFEE', 20, NULL,
  date_trunc('day', now()) - interval '1 day', date_trunc('day', now()) + interval '30 day', 'ACTIVE');

INSERT INTO promotion_items (promotion_id, coffee_id, sale_price, max_quantity)
SELECT p.id, c.id, v.sale, v.maxq FROM (VALUES
 ('Cold Brew truyền thống', 36000::numeric, 100), ('Cold Brew cam sả', NULL::numeric, 100)
) AS v(coffee, sale, maxq)
JOIN coffees c ON c.name = v.coffee JOIN promotions p ON p.name = 'Giờ vàng Cold Brew';

INSERT INTO vouchers (code, customer_id, discount_value, min_order_value, expires_at) VALUES
 ('WELCOME20K', NULL, 20000, 80000, now() + interval '90 day'),
 ('NAM50K', (SELECT id FROM customers WHERE phone = '0912345678'), 50000, 100000, now() + interval '30 day');

COMMIT;
