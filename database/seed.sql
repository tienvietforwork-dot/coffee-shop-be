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
 ('shipments',            'Giao hàng',                     '/admin/shipments',            'core',       'Đơn giao do quán tự giao: bắt đầu giao, đã giao, giao không thành công', 40),
 ('incidents',            'Sự cố đơn hàng',                '/admin/incidents',            'core',       'Xem và ghi nhận sự cố', 50),
 ('incidents.resolve',    'Xử lý sự cố & đền bù voucher',  NULL,                          'core',       NULL, 51),
 ('menu',                 'Cà phê & công thức',            '/admin/coffees',              'core',       'Xem thực đơn, công thức; bật/tắt bán, còn/hết món', 60),
 ('menu.edit',            'Sửa thực đơn & công thức',      NULL,                          'core',       'Tạo/sửa cà phê, giá, danh mục, công thức', 61),
 ('categories',           'Danh mục',                      '/admin/categories',           'core',       NULL, 62),
 ('materials',            'Nguyên liệu',                   '/admin/materials',            'core',       NULL, 70),
 ('materials.edit',       'Sửa nguyên liệu',               NULL,                          'core',       'Thêm/sửa/xóa nguyên liệu (tên, đơn vị)', 71),
 ('cash-shifts',          'Ca thu ngân',                   '/admin/cash-shifts',          'core',       'Mở / chốt ca, thu chi trong ca, bàn giao ca', 22),
 ('invoices',             'Hóa đơn điện tử',               '/admin/invoices',             'core',       'Hóa đơn từ máy tính tiền: tra cứu, thay thế, gửi lại', 23),
 ('payment-reconciliation','Đối soát thanh toán',          '/admin/payment-reconciliation','core',      'Giao dịch VNPAY, truy vấn lại, đánh dấu đã đối soát', 94),
 ('delivery-zones',       'Phí giao hàng',                 '/admin/shipments',            'core',       'Phí ship theo km, miễn phí từ, đơn tối thiểu, địa chỉ quán', 33),
 ('work-shifts',          'Phân ca & chấm công',           '/admin/work-shifts',          'core',       'Ca mẫu, xếp ca, sửa chấm công, bảng chấm công', 92),
 ('notifications',        'Tin nhắn khách hàng',           '/admin/notifications',        'core',       'Mẫu tin ZNS / SMS theo đơn, nhật ký gửi', 95),
 ('audit-logs',           'Nhật ký thao tác',              '/admin/audit-logs',           'core',       'Ai làm gì, lúc nào (chỉ xem)', 96),
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
    'dashboard', 'orders', 'pos', 'shipments', 'incidents', 'menu', 'categories', 'materials',
    'tables', 'crm.customers', 'crm.interactions', 'crm.feedbacks',
    'cash-shifts', 'invoices')
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

-- Nguyên liệu: chỉ tên + đơn vị (không theo dõi tồn kho); định lượng nằm trong công thức
INSERT INTO materials (name, unit) VALUES
 ('Kem muối', 'ml'),
 ('Cốt cold brew', 'ml'),
 ('Nước đường', 'ml'),
 ('Cốt cà phê phin', 'ml'),
 ('Siro sả', 'ml'),
 ('Cà phê Robusta rang xay', 'g'),
 ('Hạt Arabica', 'g'),
 ('Sữa đặc', 'ml'),
 ('Sữa tươi', 'ml'),
 ('Đường', 'g'),
 ('Trứng gà', 'quả'),
 ('Cốt dừa', 'ml'),
 ('Cam vàng', 'quả'),
 ('Đá viên', 'g'),
 ('Sả cây', 'g'),
 ('Whipping cream', 'ml'),
 ('Muối', 'g');

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

INSERT INTO vouchers (name, discount_value, min_order_value, quantity, valid_days) VALUES
 ('Chào mừng 20K', 20000, 80000, 100, 90),
 ('Tri ân khách hàng 50K', 50000, 100000, 50, 30),
 ('Đền bù sự cố 20K', 20000, 50000, 100, 30);

INSERT INTO voucher_details (voucher_id, code, customer_id, expires_at) VALUES
 ((SELECT id FROM vouchers WHERE name = 'Chào mừng 20K'), 'WELCOME20K', NULL, now() + interval '90 day'),
 ((SELECT id FROM vouchers WHERE name = 'Tri ân khách hàng 50K'), 'NAM50K',
  (SELECT id FROM customers WHERE phone = '0912345678'), now() + interval '30 day');

COMMIT;

-- Phí giao hàng theo km và ca làm việc mẫu
INSERT INTO delivery_settings (id, shop_address, shop_lat, shop_lng, free_ship_threshold, min_order_amount) VALUES
 (1, 'Chợ Bến Thành, Quận 1, TP. Hồ Chí Minh', 10.7725, 106.6981, 200000, 50000);
UPDATE delivery_settings SET open_time = '06:00', close_time = '22:00' WHERE id = 1;
INSERT INTO delivery_fee_plans (name, is_default) VALUES ('Mặc định', TRUE), ('Cao điểm', FALSE);
INSERT INTO delivery_fee_tiers (plan_id, from_km, to_km, fee)
SELECT p.id, v.f, v.t, CASE WHEN p.is_default THEN v.fee ELSE v.peak END FROM delivery_fee_plans p,
 (VALUES (0, 3, 15000, 20000), (3, 5, 20000, 25000), (5, 8, 30000, 35000), (8, 12, 40000, 45000)) AS v(f, t, fee, peak);
INSERT INTO delivery_time_slots (start_time, end_time, plan_id)
SELECT v.s, v.e, p.id FROM delivery_fee_plans p, (VALUES ('06:00', '08:00'), ('17:00', '20:00')) AS v(s, e) WHERE p.name = 'Cao điểm';
INSERT INTO delivery_weather_tiers (min_rain_mm, surcharge) VALUES (2.5, 5000), (8, 10000);
UPDATE delivery_settings SET weather_stop_rain_mm = 30 WHERE id = 1;
-- hóa đơn bán hàng (hộ kinh doanh), ký hiệu 2C{YY}MCH, địa chỉ = địa chỉ quán
INSERT INTO invoice_settings (id, seller_name, seller_tax_code, series_suffix, lookup_url) VALUES
 (1, 'HỘ KINH DOANH COFFEEHOLIC', '0312345678', 'CH', 'https://www.meinvoice.vn/tra-cuu');

INSERT INTO work_shifts (name, start_time, end_time, grace_minutes, color) VALUES
 ('Ca sáng', '06:30', '12:30', 5, 'blue'),
 ('Ca chiều', '12:30', '18:00', 5, 'green'),
 ('Ca tối', '18:00', '22:30', 5, 'purple');
