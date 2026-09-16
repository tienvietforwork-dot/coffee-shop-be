-- ============================================================================
-- Demo / seed data so a freshly migrated database is immediately usable.
-- Default admin login: username="admin" password="admin123" (CHANGE IN PRODUCTION)
-- The password hash below is a BCrypt hash of "admin123".
-- ============================================================================

INSERT INTO users (username, password_hash, full_name, email, phone, role, active)
VALUES ('admin', '$2a$10$WpRPP7E2O0N7XSdWniAhLOEQ9uc6AHzVDnBdh/1yAT1p8cawkQWAe', 'System Administrator', 'admin@coffeeshop.local', '0900000000', 'ADMIN', TRUE);

INSERT INTO users (username, password_hash, full_name, email, phone, role, active)
VALUES ('staff1', '$2a$10$WpRPP7E2O0N7XSdWniAhLOEQ9uc6AHzVDnBdh/1yAT1p8cawkQWAe', 'Nguyen Van Staff', 'staff1@coffeeshop.local', '0900000001', 'STAFF', TRUE);

INSERT INTO users (username, password_hash, full_name, email, phone, role, active)
VALUES ('shipper1', '$2a$10$WpRPP7E2O0N7XSdWniAhLOEQ9uc6AHzVDnBdh/1yAT1p8cawkQWAe', 'Tran Van Shipper', 'shipper1@coffeeshop.local', '0900000002', 'SHIPPER', TRUE);

INSERT INTO categories (name) VALUES ('Coffee');
INSERT INTO categories (name) VALUES ('Tea');
INSERT INTO categories (name) VALUES ('Pastry');

INSERT INTO materials (name, unit, quantity_in_stock, min_threshold, unit_price) VALUES
    ('Coffee Beans', 'kg', 20.000, 5.000, 220000),
    ('Fresh Milk', 'l', 15.000, 5.000, 32000),
    ('Sugar', 'kg', 10.000, 2.000, 18000),
    ('Tea Leaves', 'kg', 8.000, 2.000, 150000),
    ('Flour', 'kg', 12.000, 3.000, 15000);

INSERT INTO products (name, category_id, price, image_url, description, active) VALUES
    ('Black Coffee', (SELECT id FROM categories WHERE name = 'Coffee'), 25000, NULL, 'Traditional Vietnamese black coffee', TRUE),
    ('Milk Coffee', (SELECT id FROM categories WHERE name = 'Coffee'), 30000, NULL, 'Coffee with fresh milk', TRUE),
    ('Milk Tea', (SELECT id FROM categories WHERE name = 'Tea'), 28000, NULL, 'Milk tea with tea leaves', TRUE),
    ('Croissant', (SELECT id FROM categories WHERE name = 'Pastry'), 20000, NULL, 'Butter croissant', TRUE);

-- recipes (bill of materials)
INSERT INTO product_materials (product_id, material_id, quantity_required)
SELECT p.id, m.id, 0.020 FROM products p, materials m WHERE p.name = 'Black Coffee' AND m.name = 'Coffee Beans';
INSERT INTO product_materials (product_id, material_id, quantity_required)
SELECT p.id, m.id, 0.005 FROM products p, materials m WHERE p.name = 'Black Coffee' AND m.name = 'Sugar';

INSERT INTO product_materials (product_id, material_id, quantity_required)
SELECT p.id, m.id, 0.020 FROM products p, materials m WHERE p.name = 'Milk Coffee' AND m.name = 'Coffee Beans';
INSERT INTO product_materials (product_id, material_id, quantity_required)
SELECT p.id, m.id, 0.100 FROM products p, materials m WHERE p.name = 'Milk Coffee' AND m.name = 'Fresh Milk';
INSERT INTO product_materials (product_id, material_id, quantity_required)
SELECT p.id, m.id, 0.005 FROM products p, materials m WHERE p.name = 'Milk Coffee' AND m.name = 'Sugar';

INSERT INTO product_materials (product_id, material_id, quantity_required)
SELECT p.id, m.id, 0.010 FROM products p, materials m WHERE p.name = 'Milk Tea' AND m.name = 'Tea Leaves';
INSERT INTO product_materials (product_id, material_id, quantity_required)
SELECT p.id, m.id, 0.100 FROM products p, materials m WHERE p.name = 'Milk Tea' AND m.name = 'Fresh Milk';

INSERT INTO product_materials (product_id, material_id, quantity_required)
SELECT p.id, m.id, 0.080 FROM products p, materials m WHERE p.name = 'Croissant' AND m.name = 'Flour';
