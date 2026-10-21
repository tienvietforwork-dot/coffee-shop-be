-- Nhà cung cấp + phiếu đặt hàng NCC + phiếu nhập kho (mẫu 01-VT) + trả hàng nhập + phiếu chi / công nợ NCC.
-- PHẢI chạy trước khi deploy app-core có các bảng này.
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
    payment_term_days INT       NOT NULL DEFAULT 0 CHECK (payment_term_days BETWEEN 0 AND 365), -- số ngày được nợ
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
    payment_term_days INT      NOT NULL DEFAULT 0 CHECK (payment_term_days BETWEEN 0 AND 365),
    discount_amount NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (discount_amount >= 0),
    vat_rate      NUMERIC(5,2) CHECK (vat_rate BETWEEN 0 AND 100), -- % thuế GTGT hàng mua; NULL = không có HĐ GTGT
    shipping_cost NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (shipping_cost >= 0), -- chi phí nhập hàng
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
    staff_id          BIGINT       REFERENCES staff (id),           -- người nhập
    deliverer_name    VARCHAR(100),                                 -- họ tên người giao hàng
    invoice_series    VARCHAR(20),                                  -- hóa đơn nhà cung cấp: ký hiệu / số / ngày
    invoice_no        VARCHAR(20),
    invoice_date      DATE,
    document_image_id BIGINT       REFERENCES images (id),          -- ảnh phiếu giao / hóa đơn kèm theo
    discount_amount   NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (discount_amount >= 0),
    vat_rate          NUMERIC(5,2) CHECK (vat_rate BETWEEN 0 AND 100),
    shipping_cost     NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (shipping_cost >= 0),
    due_date          DATE         NOT NULL,                        -- hạn thanh toán = ngày nhập + số ngày được nợ
    status            VARCHAR(20)  NOT NULL DEFAULT 'COMPLETED' CHECK (status IN ('COMPLETED', 'CANCELLED')),
    cancel_reason     VARCHAR(255),
    cancelled_at      TIMESTAMP,
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
-- Dòng phiếu nhập: SL theo chứng từ (phiếu giao của NCC), SL thực nhập (→ lô), SL không nhận + lý do. Đơn vị gốc của nguyên liệu.
CREATE TABLE IF NOT EXISTS goods_receipt_items (
    id                     BIGSERIAL PRIMARY KEY,
    goods_receipt_id       BIGINT        NOT NULL REFERENCES goods_receipts (id),
    material_id            BIGINT        NOT NULL REFERENCES materials (id),
    purchase_order_item_id BIGINT        REFERENCES purchase_order_items (id),
    batch_id               BIGINT        REFERENCES material_batches (id), -- NULL khi không nhận gì
    package_name           VARCHAR(50),
    package_size           NUMERIC(12,2) CHECK (package_size > 0),
    document_quantity      NUMERIC(12,2) NOT NULL CHECK (document_quantity >= 0),
    received_quantity      NUMERIC(12,2) NOT NULL CHECK (received_quantity >= 0),
    rejected_quantity      NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (rejected_quantity >= 0),
    reject_reason          VARCHAR(255),
    unit_price             NUMERIC(12,2) CHECK (unit_price >= 0),    -- đ / đơn vị gốc, chưa thuế
    expiry_date            DATE,
    temperature            NUMERIC(5,1),                             -- °C đo lúc nhận hàng mát / đông
    note                   VARCHAR(255),
    created_by VARCHAR(50)  NOT NULL DEFAULT 'system',
    created_at TIMESTAMP    NOT NULL DEFAULT now(),
    updated_by VARCHAR(50),
    updated_at TIMESTAMP,
    del_flag   BOOLEAN      NOT NULL DEFAULT FALSE,
    del_user   VARCHAR(50),
    CHECK (received_quantity + rejected_quantity <= document_quantity)
);
CREATE INDEX IF NOT EXISTS idx_goods_receipt_items_receipt ON goods_receipt_items (goods_receipt_id);
CREATE INDEX IF NOT EXISTS idx_goods_receipt_items_material ON goods_receipt_items (material_id);

-- Phiếu trả hàng nhập: hàng của 1 phiếu nhập trả lại NCC. DRAFT (phiếu tạm) → COMPLETED (đã trả, trừ kho) / CANCELLED.
-- settlement: DEBT = trừ công nợ; REFUND = NCC hoàn tiền. NCC lập hóa đơn điều chỉnh giảm (adjustment_invoice_no).
CREATE TABLE IF NOT EXISTS supplier_returns (
    id                    BIGSERIAL PRIMARY KEY,
    code                  VARCHAR(20)  NOT NULL,
    supplier_id           BIGINT       NOT NULL REFERENCES suppliers (id),
    goods_receipt_id      BIGINT       NOT NULL REFERENCES goods_receipts (id),
    staff_id              BIGINT       REFERENCES staff (id),
    status                VARCHAR(20)  NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT', 'COMPLETED', 'CANCELLED')),
    reason                VARCHAR(255) NOT NULL,
    settlement            VARCHAR(20)  NOT NULL DEFAULT 'DEBT' CHECK (settlement IN ('DEBT', 'REFUND')),
    refund_method         VARCHAR(20)  CHECK (refund_method IN ('CASH', 'BANK_TRANSFER')),
    vat_rate              NUMERIC(5,2) CHECK (vat_rate BETWEEN 0 AND 100),
    adjustment_invoice_no VARCHAR(30),
    completed_at          TIMESTAMP,
    cancel_reason         VARCHAR(255),
    note                  VARCHAR(255),
    created_by VARCHAR(50)  NOT NULL DEFAULT 'system',
    created_at TIMESTAMP    NOT NULL DEFAULT now(),
    updated_by VARCHAR(50),
    updated_at TIMESTAMP,
    del_flag   BOOLEAN      NOT NULL DEFAULT FALSE,
    del_user   VARCHAR(50)
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_supplier_returns_code ON supplier_returns (code) WHERE NOT del_flag;
CREATE INDEX IF NOT EXISTS idx_supplier_returns_supplier ON supplier_returns (supplier_id);
CREATE INDEX IF NOT EXISTS idx_supplier_returns_receipt ON supplier_returns (goods_receipt_id);

CREATE TABLE IF NOT EXISTS supplier_return_items (
    id                    BIGSERIAL PRIMARY KEY,
    supplier_return_id    BIGINT        NOT NULL REFERENCES supplier_returns (id),
    goods_receipt_item_id BIGINT        NOT NULL REFERENCES goods_receipt_items (id),
    quantity              NUMERIC(12,2) NOT NULL CHECK (quantity > 0),
    unit_price            NUMERIC(12,2) NOT NULL CHECK (unit_price >= 0),
    note                  VARCHAR(255),
    created_by VARCHAR(50)  NOT NULL DEFAULT 'system',
    created_at TIMESTAMP    NOT NULL DEFAULT now(),
    updated_by VARCHAR(50),
    updated_at TIMESTAMP,
    del_flag   BOOLEAN      NOT NULL DEFAULT FALSE,
    del_user   VARCHAR(50)
);
CREATE INDEX IF NOT EXISTS idx_supplier_return_items_return ON supplier_return_items (supplier_return_id);
CREATE INDEX IF NOT EXISTS idx_supplier_return_items_line ON supplier_return_items (goods_receipt_item_id);

-- Phiếu chi trả nhà cung cấp (mẫu 02-TT). goods_receipt_id: trả cho phiếu nhập đó; purchase_order_id: trả trước / đặt cọc.
CREATE TABLE IF NOT EXISTS supplier_payments (
    id                BIGSERIAL PRIMARY KEY,
    code              VARCHAR(20)   NOT NULL,
    supplier_id       BIGINT        NOT NULL REFERENCES suppliers (id),
    goods_receipt_id  BIGINT        REFERENCES goods_receipts (id),
    purchase_order_id BIGINT        REFERENCES purchase_orders (id),
    amount            NUMERIC(12,2) NOT NULL CHECK (amount > 0),
    method            VARCHAR(20)   NOT NULL CHECK (method IN ('CASH', 'BANK_TRANSFER')),
    reference         VARCHAR(100),                                -- mã giao dịch chuyển khoản
    payee_name        VARCHAR(100),                                -- người nhận tiền
    paid_at           TIMESTAMP     NOT NULL,
    staff_id          BIGINT        REFERENCES staff (id),         -- người lập phiếu
    document_image_id BIGINT        REFERENCES images (id),
    status            VARCHAR(20)   NOT NULL DEFAULT 'COMPLETED' CHECK (status IN ('COMPLETED', 'CANCELLED')),
    cancel_reason     VARCHAR(255),
    note              VARCHAR(255),
    created_by VARCHAR(50)  NOT NULL DEFAULT 'system',
    created_at TIMESTAMP    NOT NULL DEFAULT now(),
    updated_by VARCHAR(50),
    updated_at TIMESTAMP,
    del_flag   BOOLEAN      NOT NULL DEFAULT FALSE,
    del_user   VARCHAR(50)
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_supplier_payments_code ON supplier_payments (code) WHERE NOT del_flag;
CREATE INDEX IF NOT EXISTS idx_supplier_payments_supplier ON supplier_payments (supplier_id);

-- Hủy phiếu nhập và trả hàng nhập ghi giao dịch kho riêng
ALTER TABLE material_transactions DROP CONSTRAINT IF EXISTS material_transactions_type_check;
ALTER TABLE material_transactions ADD CONSTRAINT material_transactions_type_check CHECK (type IN
    ('IMPORT', 'EXPORT', 'SALE', 'ADJUSTMENT', 'PRODUCTION_USE', 'PRODUCE', 'DISPOSAL', 'IMPORT_CANCEL', 'SUPPLIER_RETURN'));

-- Quyền màn hình (khớp Perm.java / src/lib/perm.ts); Quản lý có mọi quyền quản trị.
-- Nhân viên giữ quyền 'stock': xem phiếu đặt đang chờ và nhận hàng ở màn Nhập / xuất kho.
INSERT INTO permissions (code, name, path, module, description, sort_order)
SELECT v.code, v.name, v.path, 'core', v.description, v.sort_order FROM (VALUES
 ('suppliers',       'Nhà cung cấp',          '/admin/suppliers',       'Nhà cung cấp, nguyên liệu họ bán, giá tham khảo', 74),
 ('purchase-orders', 'Phiếu đặt hàng',        '/admin/purchase-orders', 'Lập, gửi, hủy, đóng phiếu đặt hàng nhà cung cấp; hủy phiếu nhập', 75),
 ('supplier-returns', 'Trả hàng nhập',        '/admin/supplier-returns', 'Phiếu trả hàng nhà cung cấp', 76),
 ('supplier-debts',  'Công nợ nhà cung cấp',  '/admin/supplier-debts',  'Công nợ phải trả, sổ chi tiết, phiếu chi', 77)
) AS v(code, name, path, description, sort_order)
WHERE NOT EXISTS (SELECT 1 FROM permissions p WHERE p.code = v.code AND NOT p.del_flag);

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON p.code IN ('suppliers', 'purchase-orders', 'supplier-returns', 'supplier-debts') AND NOT p.del_flag
WHERE r.code = 'ADMIN'
  AND NOT EXISTS (SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id AND NOT rp.del_flag);

COMMIT;
