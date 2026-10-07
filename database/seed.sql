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

-- Nguyên liệu thô (RAW, nhập mua) và bán thành phẩm (PREPARED, tự chế biến theo định lượng chuẩn)
INSERT INTO materials (name, unit, min_stock, kind, yield_quantity, prep_minutes, shelf_life_minutes, instructions) VALUES
 ('Kem muối', 'ml', 500, 'PREPARED', 900, 15, 2880, '1. Để whipping cream, âu và cây đánh trứng trong ngăn mát ít nhất 15 phút (càng lạnh càng dễ bông).
2. Cho 500 ml whipping cream, 120 ml sữa đặc, 60 ml sữa tươi và 5 g muối vào âu.
3. Đánh tốc độ vừa 3–5 phút đến khi kem sánh, chảy thành dải mềm (bông mềm) — không đánh cứng, sẽ không nổi đều trên ly.
4. Nếm thử: vị béo, mặn nhẹ cuối lưỡi; thiếu mặn thêm 1 nhúm muối.
5. Cho vào hộp kín, dán nhãn lô + hạn dùng, bảo quản ngăn mát 2–4 °C, dùng trong 2 ngày.
6. Trước khi dùng khuấy nhẹ lại vài vòng nếu kem bị tách.'),
 ('Cốt cold brew', 'ml', 1000, 'PREPARED', 1000, 1080, 10080, '1. Cân 250 g hạt Arabica, xay cỡ thô (hạt như muối hột) ngay trước khi ủ.
2. Cho bột vào túi lọc vải, đặt trong bình ủ sạch có nắp.
3. Rót 1.500 ml nước lọc ở nhiệt độ phòng, khuấy nhẹ 10–15 giây để bột ngấm đều, không khuấy mạnh.
4. Đậy kín, ủ ngăn mát 2–4 °C trong 16–18 giờ.
5. Nhấc túi lọc ra, để ráo tự nhiên 5 phút, không vắt (vắt sẽ làm cốt đắng và đục).
6. Lọc lại qua giấy lọc, đóng chai sạch.
7. Dán nhãn số lô + hạn dùng, bảo quản ngăn mát 2–4 °C, dùng trong 7 ngày.
Tỉ lệ cốt đậm 1:6 — bã giữ lại khoảng 2 ml nước/g nên thu khoảng 1.000 ml.'),
 ('Nước đường', 'ml', 500, 'PREPARED', 1300, 60, 20160, '1. Cho 1.000 g đường cát trắng và 700 ml nước lọc vào nồi inox.
2. Đun lửa vừa, khuấy đều đến khi đường tan hết và nước trong (khoảng 5–7 phút), không để sôi lâu kẻo bị keo.
3. Tắt bếp, để nguội hoàn toàn ở nhiệt độ phòng (khoảng 1 giờ).
4. Rót vào chai sạch có nắp, dán nhãn số lô + hạn dùng.
5. Bảo quản ngăn mát, dùng trong 14 ngày; bỏ đi nếu đục hoặc có mùi chua.
Tỉ lệ 1 đường : 0,7 nước thu khoảng 1.300 ml.'),
 ('Cốt cà phê phin', 'ml', 500, 'PREPARED', 500, 30, 1440, '1. Tráng phin lớn / bình pha bằng nước sôi cho nóng đều.
2. Cho 250 g bột Robusta, lắc phẳng mặt, nén nhẹ.
3. Rót khoảng 100 ml nước 92–96 °C, ủ 30–45 giây cho bột nở.
4. Rót tiếp 700 ml nước, đậy nắp, để nhỏ giọt hết (khoảng 20–25 phút).
5. Để nguội bớt, rót vào bình thủy tinh sạch có nắp, dán nhãn lô + giờ pha.
6. Bảo quản ngăn mát, dùng trong 24 giờ; lắc nhẹ trước khi rót.
Tỉ lệ khoảng 1:3,2 — bã giữ khoảng 1,2 ml nước/g nên thu khoảng 500 ml cốt.'),
 ('Siro sả', 'ml', 300, 'PREPARED', 800, 60, 10080, '1. Rửa 200 g sả (khoảng 6–8 cây), bỏ lá già ngoài, cắt khúc 3 cm, đập dập.
2. Cho sả, 500 g đường và 500 ml nước vào nồi, đun sôi.
3. Hạ lửa nhỏ, đun liu riu 10 phút, khuấy thỉnh thoảng.
4. Tắt bếp, ngâm sả trong siro đến khi nguội hẳn (khoảng 40 phút) để ra mùi.
5. Lọc bỏ bã, rót vào chai sạch có nắp, dán nhãn lô + hạn dùng.
6. Bảo quản ngăn mát, dùng trong 7 ngày; bỏ đi nếu siro đục.'),
 ('Cà phê Robusta rang xay', 'g', 2000, 'RAW', NULL, NULL, NULL, NULL),
 ('Hạt Arabica', 'g', 1500, 'RAW', NULL, NULL, NULL, NULL),
 ('Sữa đặc', 'ml', 2000, 'RAW', NULL, NULL, NULL, NULL),
 ('Sữa tươi', 'ml', 3000, 'RAW', NULL, NULL, NULL, NULL),
 ('Đường', 'g', 1000, 'RAW', NULL, NULL, NULL, NULL),
 ('Trứng gà', 'quả', 20, 'RAW', NULL, NULL, NULL, NULL),
 ('Cốt dừa', 'ml', 1000, 'RAW', NULL, NULL, NULL, NULL),
 ('Cam vàng', 'quả', 10, 'RAW', NULL, NULL, NULL, NULL),
 ('Đá viên', 'g', 5000, 'RAW', NULL, NULL, NULL, NULL),
 ('Sả cây', 'g', 500, 'RAW', NULL, NULL, NULL, NULL),
 ('Whipping cream', 'ml', 1000, 'RAW', NULL, NULL, NULL, NULL),
 ('Muối', 'g', 200, 'RAW', NULL, NULL, NULL, NULL);

-- Định lượng chuẩn của bán thành phẩm
INSERT INTO material_components (material_id, component_id, quantity)
SELECT m.id, k.id, v.q FROM (VALUES
 ('Cốt cold brew', 'Hạt Arabica', 250),
 ('Cốt cà phê phin', 'Cà phê Robusta rang xay', 250),
 ('Kem muối', 'Muối', 5),
 ('Kem muối', 'Sữa tươi', 60),
 ('Kem muối', 'Sữa đặc', 120),
 ('Kem muối', 'Whipping cream', 500),
 ('Nước đường', 'Đường', 1000),
 ('Siro sả', 'Sả cây', 200),
 ('Siro sả', 'Đường', 500)
) AS v(mat, comp, q)
JOIN materials m ON m.name = v.mat JOIN materials k ON k.name = v.comp;

-- Lô đầu kỳ: nguyên liệu thô nhập mua; bán thành phẩm là tồn đầu kỳ đã chế biến sẵn (hạn dùng theo shelf_life_minutes)
INSERT INTO material_batches (material_id, import_quantity, remaining_quantity, unit_cost, expiry_date)
SELECT m.id, v.q, v.q, v.c, CURRENT_DATE + v.d FROM (VALUES
 ('Cà phê Robusta rang xay', 8000, 0.25, 60), ('Hạt Arabica', 5000, 0.45, 90), ('Sữa đặc', 6000, 0.06, 120),
 ('Sữa tươi', 8000, 0.035, 5), ('Đường', 5000, 0.025, 365), ('Trứng gà', 60, 3500, 10),
 ('Cốt dừa', 800, 0.09, 30), ('Cam vàng', 30, 9000, 7), ('Đá viên', 30000, 0.002, 2),
 ('Sả cây', 3000, 0.04, 7), ('Whipping cream', 4000, 0.12, 30), ('Muối', 1000, 0.01, 365)
) AS v(name, q, c, d) JOIN materials m ON m.name = v.name;

INSERT INTO material_batches (material_id, import_quantity, remaining_quantity, expiry_date, expires_at, ready_at)
SELECT m.id, v.q, v.q, (now() + m.shelf_life_minutes * interval '1 minute')::date,
       now() + m.shelf_life_minutes * interval '1 minute', now()
FROM (VALUES
 ('Cốt cold brew', 2000), ('Cốt cà phê phin', 1000), ('Kem muối', 900), ('Nước đường', 1300), ('Siro sả', 800)
) AS v(name, q) JOIN materials m ON m.name = v.name;

UPDATE materials m SET stock_quantity = b.total
FROM (SELECT material_id, SUM(remaining_quantity) total FROM material_batches GROUP BY material_id) b
WHERE b.material_id = m.id;

INSERT INTO material_transactions (batch_id, staff_id, type, quantity, note)
SELECT b.id, (SELECT id FROM staff WHERE phone = '0901000001'),
       CASE m.kind WHEN 'PREPARED' THEN 'PRODUCE' ELSE 'IMPORT' END, b.import_quantity,
       CASE m.kind WHEN 'PREPARED' THEN 'Tồn đầu kỳ (đã chế biến sẵn)' ELSE 'Nhập kho ban đầu' END
FROM material_batches b JOIN materials m ON m.id = b.material_id;

INSERT INTO recipes (coffee_id, brew_method, version, brew_time_min, description, is_active)
SELECT c.id, v.bm, 1, v.t, v.d, TRUE FROM (VALUES
 ('Bạc xỉu', 'PHIN', 2, NULL),
 ('Cà phê sữa đá', 'PHIN', 1, NULL),
 ('Cà phê đen đá', 'PHIN', 1, NULL),
 ('Latte', 'MACHINE', 4, NULL),
 ('Cappuccino', 'MACHINE', 4, NULL),
 ('Americano', 'MACHINE', 3, 'cơ bản'),
 ('Espresso', 'MACHINE', 2, NULL),
 ('Cold Brew cam sả', 'COLD_BREW', 2, NULL),
 ('Cold Brew truyền thống', 'COLD_BREW', 2, NULL),
 ('Cà phê dừa', 'PHIN', 3, NULL),
 ('Cà phê trứng', 'PHIN', 5, NULL),
 ('Cà phê muối', 'PHIN', 2, NULL)
) AS v(name, bm, t, d) JOIN coffees c ON c.name = v.name;

INSERT INTO recipe_materials (recipe_id, material_id, quantity, note)
SELECT r.id, m.id, v.q, v.note FROM (VALUES
 ('Bạc xỉu', 'Cốt cà phê phin', 25, NULL),
 ('Bạc xỉu', 'Sữa đặc', 30, NULL),
 ('Bạc xỉu', 'Sữa tươi', 60, NULL),
 ('Bạc xỉu', 'Đá viên', 150, NULL),
 ('Cà phê sữa đá', 'Cốt cà phê phin', 50, NULL),
 ('Cà phê sữa đá', 'Sữa đặc', 30, NULL),
 ('Cà phê sữa đá', 'Đá viên', 150, NULL),
 ('Cà phê đen đá', 'Cốt cà phê phin', 50, NULL),
 ('Cà phê đen đá', 'Nước đường', 15, NULL),
 ('Cà phê đen đá', 'Đá viên', 150, NULL),
 ('Latte', 'Hạt Arabica', 18, NULL),
 ('Latte', 'Sữa tươi', 180, NULL),
 ('Cappuccino', 'Hạt Arabica', 18, NULL),
 ('Cappuccino', 'Sữa tươi', 120, NULL),
 ('Americano', 'Hạt Arabica', 18, NULL),
 ('Americano', 'Đá viên', 100, NULL),
 ('Espresso', 'Hạt Arabica', 18, NULL),
 ('Cold Brew cam sả', 'Cốt cold brew', 70, NULL),
 ('Cold Brew cam sả', 'Siro sả', 15, NULL),
 ('Cold Brew cam sả', 'Cam vàng', 0.5, NULL),
 ('Cold Brew cam sả', 'Đá viên', 150, NULL),
 ('Cold Brew truyền thống', 'Cốt cold brew', 80, NULL),
 ('Cold Brew truyền thống', 'Đá viên', 150, NULL),
 ('Cà phê dừa', 'Cốt cà phê phin', 40, NULL),
 ('Cà phê dừa', 'Cốt dừa', 60, NULL),
 ('Cà phê dừa', 'Sữa đặc', 20, NULL),
 ('Cà phê dừa', 'Đá viên', 200, NULL),
 ('Cà phê trứng', 'Cốt cà phê phin', 40, NULL),
 ('Cà phê trứng', 'Trứng gà', 1, 'trứng tiệt trùng'),
 ('Cà phê trứng', 'Sữa đặc', 25, NULL),
 ('Cà phê muối', 'Cốt cà phê phin', 50, NULL),
 ('Cà phê muối', 'Sữa đặc', 20, NULL),
 ('Cà phê muối', 'Kem muối', 40, NULL),
 ('Cà phê muối', 'Đá viên', 120, NULL)
) AS v(coffee, material, q, note)
JOIN coffees c ON c.name = v.coffee
JOIN recipes r ON r.coffee_id = c.id AND r.is_active
JOIN materials m ON m.name = v.material;

INSERT INTO recipe_steps (recipe_id, step_no, instruction)
SELECT r.id, v.n, v.s FROM (VALUES
 ('Bạc xỉu', 1, 'Rót 30 ml sữa đặc và 60 ml sữa tươi vào ly, khuấy đều'),
 ('Bạc xỉu', 2, 'Cho 150 g đá'),
 ('Bạc xỉu', 3, 'Rót từ từ 25 ml cốt phin lên mặt đá để tạo lớp nâu trên nền trắng'),
 ('Bạc xỉu', 4, 'Phục vụ kèm muỗng, dặn khách khuấy trước khi uống'),
 ('Cà phê sữa đá', 1, 'Lắc nhẹ bình cốt phin, kiểm tra giờ pha trên nhãn'),
 ('Cà phê sữa đá', 2, 'Rót 30 ml sữa đặc vào ly'),
 ('Cà phê sữa đá', 3, 'Thêm 50 ml cốt phin, khuấy đến khi sữa tan đều, màu nâu sữa'),
 ('Cà phê sữa đá', 4, 'Cho 150 g đá, khuấy nhẹ rồi phục vụ'),
 ('Cà phê đen đá', 1, 'Lắc nhẹ bình cốt phin, kiểm tra giờ pha trên nhãn (dùng trong 24 giờ)'),
 ('Cà phê đen đá', 2, 'Rót 15 ml nước đường vào ly (khách dặn ít ngọt thì giảm còn 8 ml)'),
 ('Cà phê đen đá', 3, 'Thêm 50 ml cốt phin, khuấy đều'),
 ('Cà phê đen đá', 4, 'Cho 150 g đá, khuấy nhẹ 2–3 vòng rồi phục vụ'),
 ('Latte', 1, 'Chiết xuất 1 shot espresso 18g / 36ml trong 28 giây'),
 ('Latte', 2, 'Đánh nóng 180ml sữa tươi tới 60°C'),
 ('Latte', 3, 'Rót sữa vào espresso, tạo latte art'),
 ('Americano', 1, 'Xay pha mịn 18g hạt arabica, sau đó chiết xuất từ máy trong 30s'),
 ('Americano', 2, 'Chuẩn bị ly vừa với 100g đá'),
 ('Americano', 3, 'Rót trực tiếp vào đá rồi thêm 100ml nước lạnh'),
 ('Cold Brew cam sả', 1, 'Rót 15 ml siro sả vào ly 400 ml'),
 ('Cold Brew cam sả', 2, 'Vắt ½ quả cam vàng, lọc hạt, rót nước cam vào ly'),
 ('Cold Brew cam sả', 3, 'Cho 150 g đá'),
 ('Cold Brew cam sả', 4, 'Rót chậm 70 ml cốt cold brew lên trên để tạo 2 lớp màu'),
 ('Cold Brew cam sả', 5, 'Trang trí 1 lát cam, dặn khách khuấy đều trước khi uống'),
 ('Cold Brew truyền thống', 1, 'Lắc nhẹ chai cốt cold brew, kiểm tra hạn dùng trên nhãn lô'),
 ('Cold Brew truyền thống', 2, 'Cho 150 g đá vào ly 400 ml'),
 ('Cold Brew truyền thống', 3, 'Rót 80 ml cốt cold brew lên đá'),
 ('Cold Brew truyền thống', 4, 'Thêm 80 ml nước lọc lạnh, khuấy nhẹ 3–4 vòng'),
 ('Cold Brew truyền thống', 5, 'Hỏi khách có cần thêm nước đường / sữa không, phục vụ ngay'),
 ('Cà phê dừa', 1, 'Cho 60 ml cốt dừa, 20 ml sữa đặc và 200 g đá vào máy xay'),
 ('Cà phê dừa', 2, 'Xay 20–30 giây đến khi sánh mịn như tuyết'),
 ('Cà phê dừa', 3, 'Đổ hỗn hợp dừa vào ly'),
 ('Cà phê dừa', 4, 'Rót 40 ml cốt phin lên trên, phục vụ kèm muỗng'),
 ('Cà phê trứng', 1, 'Hâm nóng 40 ml cốt phin tới khoảng 70 °C, rót vào tách; đặt tách trong bát nước nóng để giữ nhiệt'),
 ('Cà phê trứng', 2, 'Tách lấy lòng đỏ 1 quả trứng tiệt trùng, thêm 25 ml sữa đặc'),
 ('Cà phê trứng', 3, 'Đánh bằng máy đánh trứng 3–4 phút đến khi kem bông mịn, màu vàng nhạt, nhấc lên chảy thành dải'),
 ('Cà phê trứng', 4, 'Rót kem trứng phủ lên mặt cà phê, phục vụ ngay kèm muỗng'),
 ('Cà phê muối', 1, 'Rót 20 ml sữa đặc vào ly, cho 120 g đá'),
 ('Cà phê muối', 2, 'Thêm 50 ml cốt phin'),
 ('Cà phê muối', 3, 'Khuấy nhẹ hộp kem muối cho đều (kem để ngăn mát, dùng trong 2 ngày)'),
 ('Cà phê muối', 4, 'Rót 40 ml kem muối phủ kín mặt ly, không khuấy; phục vụ ngay để giữ lớp kem')
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
