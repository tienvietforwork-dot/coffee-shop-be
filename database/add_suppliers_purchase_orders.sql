-- Nhà cung cấp + phiếu đặt hàng nhà cung cấp + phiếu nhập kho. PHẢI chạy trước khi deploy app-core có các bảng này.
-- Luồng: phiếu đặt (Nháp → Đã đặt → Nhận một phần → Hoàn tất / Đã hủy) → mỗi lần nhận hàng = 1 phiếu nhập,
-- mỗi dòng nhận về = 1 lô (material_batches). Nhập lẻ không cần phiếu đặt vẫn tạo phiếu nhập, bắt buộc có nhà cung cấp.
BEGIN;

CREATE TABLE IF NOT EXISTS suppliers (
    id           BIGSERIAL PRIMARY KEY,
    name         VARCHAR(100) NOT NULL,
    contact_name VARCHAR(100),
    phone        VARCHAR(15),
    email        VARCHAR(100),
    address      VARCHAR(255),
    tax_code     VARCHAR(20),
    note         VARCHAR(255),
    -- INACTIVE = ngừng hợp tác (phiếu cũ vẫn tham chiếu nên không xóa)
    status       VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE')),
    created_by VARCHAR(50)  NOT NULL DEFAULT 'system',
    created_at TIMESTAMP    NOT NULL DEFAULT now(),
    updated_by VARCHAR(50),
    updated_at TIMESTAMP,
    del_flag   BOOLEAN      NOT NULL DEFAULT FALSE,
    del_user   VARCHAR(50)
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_suppliers_name ON suppliers (lower(name)) WHERE NOT del_flag;

-- Nguyên liệu nhà cung cấp bán: quy cách hay đặt + giá tham khảo (đ / đơn vị gốc, như material_batches.unit_cost)
CREATE TABLE IF NOT EXISTS supplier_materials (
    id          BIGSERIAL PRIMARY KEY,
    supplier_id BIGINT        NOT NULL REFERENCES suppliers (id),
    material_id BIGINT        NOT NULL REFERENCES materials (id),
    package_id  BIGINT        REFERENCES material_packages (id),
    unit_price  NUMERIC(12,2) CHECK (unit_price >= 0),
    created_by VARCHAR(50)  NOT NULL DEFAULT 'system',
    created_at TIMESTAMP    NOT NULL DEFAULT now(),
    updated_by VARCHAR(50),
    updated_at TIMESTAMP,
    del_flag   BOOLEAN      NOT NULL DEFAULT FALSE,
    del_user   VARCHAR(50)
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_supplier_materials ON supplier_materials (supplier_id, material_id) WHERE NOT del_flag;
CREATE INDEX IF NOT EXISTS idx_supplier_materials_material ON supplier_materials (material_id);

CREATE TABLE IF NOT EXISTS purchase_orders (
    id            BIGSERIAL PRIMARY KEY,
    code          VARCHAR(20)  NOT NULL,
    supplier_id   BIGINT       NOT NULL REFERENCES suppliers (id),
    staff_id      BIGINT       REFERENCES staff (id),           -- người lập phiếu
    status        VARCHAR(20)  NOT NULL DEFAULT 'DRAFT'
                  CHECK (status IN ('DRAFT', 'ORDERED', 'PARTIALLY_RECEIVED', 'COMPLETED', 'CANCELLED')),
    expected_date DATE,                                         -- ngày hẹn giao
    ordered_at    TIMESTAMP,                                    -- lúc gửi nhà cung cấp
    completed_at  TIMESTAMP,                                    -- nhận đủ / đóng phiếu / hủy
    note          VARCHAR(255),
    close_reason  VARCHAR(255),                                 -- đóng khi chưa nhận đủ, hoặc lý do hủy
    created_by VARCHAR(50)  NOT NULL DEFAULT 'system',
    created_at TIMESTAMP    NOT NULL DEFAULT now(),
    updated_by VARCHAR(50),
    updated_at TIMESTAMP,
    del_flag   BOOLEAN      NOT NULL DEFAULT FALSE,
    del_user   VARCHAR(50)
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_purchase_orders_code ON purchase_orders (code) WHERE NOT del_flag;
CREATE INDEX IF NOT EXISTS idx_purchase_orders_supplier ON purchase_orders (supplier_id);
CREATE INDEX IF NOT EXISTS idx_purchase_orders_status ON purchase_orders (status);

-- Dòng phiếu đặt. quantity / received_quantity theo đơn vị gốc của nguyên liệu; quy cách chụp lại lúc đặt.
CREATE TABLE IF NOT EXISTS purchase_order_items (
    id                BIGSERIAL PRIMARY KEY,
    purchase_order_id BIGINT        NOT NULL REFERENCES purchase_orders (id),
    material_id       BIGINT        NOT NULL REFERENCES materials (id),
    package_name      VARCHAR(50),
    package_size      NUMERIC(12,2) CHECK (package_size > 0),
    package_count     NUMERIC(12,2) CHECK (package_count > 0),
    quantity          NUMERIC(12,2) NOT NULL CHECK (quantity > 0),
    received_quantity NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (received_quantity >= 0),
    unit_price        NUMERIC(12,2) CHECK (unit_price >= 0),   -- đ / đơn vị gốc
    note              VARCHAR(255),
    created_by VARCHAR(50)  NOT NULL DEFAULT 'system',
    created_at TIMESTAMP    NOT NULL DEFAULT now(),
    updated_by VARCHAR(50),
    updated_at TIMESTAMP,
    del_flag   BOOLEAN      NOT NULL DEFAULT FALSE,
    del_user   VARCHAR(50)
);
CREATE INDEX IF NOT EXISTS idx_purchase_order_items_order ON purchase_order_items (purchase_order_id);
CREATE INDEX IF NOT EXISTS idx_purchase_order_items_material ON purchase_order_items (material_id);

-- Phiếu nhập kho: 1 lần nhận hàng (theo phiếu đặt, hoặc nhập lẻ khi purchase_order_id NULL). Không sửa / xóa.
CREATE TABLE IF NOT EXISTS goods_receipts (
    id                BIGSERIAL PRIMARY KEY,
    code              VARCHAR(20)  NOT NULL,
    supplier_id       BIGINT       NOT NULL REFERENCES suppliers (id),
    purchase_order_id BIGINT       REFERENCES purchase_orders (id),
    staff_id          BIGINT       REFERENCES staff (id),
    note              VARCHAR(255),
    created_by VARCHAR(50)  NOT NULL DEFAULT 'system',
    created_at TIMESTAMP    NOT NULL DEFAULT now(),
    updated_by VARCHAR(50),
    updated_at TIMESTAMP,
    del_flag   BOOLEAN      NOT NULL DEFAULT FALSE,
    del_user   VARCHAR(50)
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_goods_receipts_code ON goods_receipts (code) WHERE NOT del_flag;
CREATE INDEX IF NOT EXISTS idx_goods_receipts_supplier ON goods_receipts (supplier_id);
CREATE INDEX IF NOT EXISTS idx_goods_receipts_order ON goods_receipts (purchase_order_id);

-- Mỗi dòng phiếu nhập là 1 lô
ALTER TABLE material_batches ADD COLUMN IF NOT EXISTS goods_receipt_id       BIGINT REFERENCES goods_receipts (id);
ALTER TABLE material_batches ADD COLUMN IF NOT EXISTS purchase_order_item_id BIGINT REFERENCES purchase_order_items (id);
CREATE INDEX IF NOT EXISTS idx_material_batches_receipt ON material_batches (goods_receipt_id);
CREATE INDEX IF NOT EXISTS idx_material_batches_po_item ON material_batches (purchase_order_item_id);

-- Quyền màn hình (khớp Perm.java / src/lib/perm.ts); Quản lý có mọi quyền quản trị.
-- Nhân viên giữ quyền 'stock': xem phiếu đặt đang chờ và nhận hàng ở màn Nhập / xuất kho.
INSERT INTO permissions (code, name, path, module, description, sort_order)
SELECT v.code, v.name, v.path, 'core', v.description, v.sort_order FROM (VALUES
 ('suppliers',       'Nhà cung cấp',          '/admin/suppliers',       'Nhà cung cấp, nguyên liệu họ bán, giá tham khảo', 74),
 ('purchase-orders', 'Phiếu đặt hàng',        '/admin/purchase-orders', 'Lập, gửi, hủy, đóng phiếu đặt hàng nhà cung cấp', 75)
) AS v(code, name, path, description, sort_order)
WHERE NOT EXISTS (SELECT 1 FROM permissions p WHERE p.code = v.code AND NOT p.del_flag);

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON p.code IN ('suppliers', 'purchase-orders') AND NOT p.del_flag
WHERE r.code = 'ADMIN'
  AND NOT EXISTS (SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id AND NOT rp.del_flag);

COMMIT;
