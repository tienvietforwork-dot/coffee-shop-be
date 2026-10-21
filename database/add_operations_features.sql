-- Kiểm kê, báo cáo xuất – nhập – tồn, giá vốn món, ca thu ngân, phân ca & chấm công, nhật ký thao tác,
-- thanh toán online & đối soát, giao hàng qua đối tác & phí ship theo khu vực, hóa đơn điện tử, tin nhắn ZNS / SMS.
-- Chạy SAU add_suppliers_purchase_orders.sql, TRƯỚC khi deploy app-core có các chức năng này. Chạy lại được (IF NOT EXISTS).
BEGIN;

-- ---------------------------------------------------------------------
-- Phiếu kiểm kê (biên bản kiểm kê vật tư, mẫu 05-VT): DRAFT (đang đếm) → BALANCED (đã cân bằng kho) / CANCELLED
-- book_quantity = SL theo sổ lúc đưa lô vào phiếu; cân bằng kho dịch lô theo chênh lệch (material_transactions ADJUSTMENT)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS stocktakes (
    id          BIGSERIAL PRIMARY KEY,
    code        VARCHAR(20)  NOT NULL,
    status      VARCHAR(20)  NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT', 'BALANCED', 'CANCELLED')),
    staff_id    BIGINT       REFERENCES staff (id),          -- trưởng ban kiểm kê
    members     VARCHAR(255),                                -- người cùng kiểm
    counted_at  TIMESTAMP    NOT NULL,
    balanced_by BIGINT       REFERENCES staff (id),
    balanced_at TIMESTAMP,
    resolution  VARCHAR(255),                                -- ý kiến giải quyết số chênh lệch
    note        VARCHAR(255),
    created_by VARCHAR(50)  NOT NULL DEFAULT 'system',
    created_at TIMESTAMP    NOT NULL DEFAULT now(),
    updated_by VARCHAR(50),
    updated_at TIMESTAMP,
    del_flag   BOOLEAN      NOT NULL DEFAULT FALSE,
    del_user   VARCHAR(50)
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_stocktakes_code ON stocktakes (code) WHERE NOT del_flag;

CREATE TABLE IF NOT EXISTS stocktake_items (
    id               BIGSERIAL PRIMARY KEY,
    stocktake_id     BIGINT        NOT NULL REFERENCES stocktakes (id),
    batch_id         BIGINT        NOT NULL REFERENCES material_batches (id),
    book_quantity    NUMERIC(12,2) NOT NULL CHECK (book_quantity >= 0),
    counted_quantity NUMERIC(12,2) CHECK (counted_quantity >= 0),  -- NULL = chưa đếm
    unit_cost        NUMERIC(12,2),
    quality          VARCHAR(10)   NOT NULL DEFAULT 'GOOD' CHECK (quality IN ('GOOD', 'POOR', 'LOST')), -- phẩm chất
    reason           VARCHAR(255),                                 -- nguyên nhân chênh lệch
    created_by VARCHAR(50)  NOT NULL DEFAULT 'system',
    created_at TIMESTAMP    NOT NULL DEFAULT now(),
    updated_by VARCHAR(50),
    updated_at TIMESTAMP,
    del_flag   BOOLEAN      NOT NULL DEFAULT FALSE,
    del_user   VARCHAR(50)
);
CREATE INDEX IF NOT EXISTS idx_stocktake_items_stocktake ON stocktake_items (stocktake_id);

-- Tồn kho chốt cuối ngày (job 23:55), cho biểu đồ tồn theo ngày
CREATE TABLE IF NOT EXISTS inventory_snapshots (
    id            BIGSERIAL PRIMARY KEY,
    snapshot_date DATE          NOT NULL,
    material_id   BIGINT        NOT NULL REFERENCES materials (id),
    quantity      NUMERIC(14,2) NOT NULL,
    stock_value   NUMERIC(14,2) NOT NULL,
    created_at    TIMESTAMP     NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_inventory_snapshots ON inventory_snapshots (snapshot_date, material_id);

-- Giá vốn món theo định lượng: 1 dòng mỗi khi giá vốn hoặc giá bán đổi (job mỗi giờ)
CREATE TABLE IF NOT EXISTS coffee_cost_snapshots (
    id          BIGSERIAL PRIMARY KEY,
    coffee_id   BIGINT        NOT NULL REFERENCES coffees (id),
    recipe_id   BIGINT        REFERENCES recipes (id),
    unit_cost   NUMERIC(12,2) NOT NULL,
    price       NUMERIC(12,2) NOT NULL,
    computed_at TIMESTAMP     NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_coffee_cost_snapshots_coffee ON coffee_cost_snapshots (coffee_id, computed_at);

-- ---------------------------------------------------------------------
-- Ca thu ngân (mở / chốt ca, bàn giao ca). 1 két: chỉ 1 ca đang mở. Số liệu bán hàng chốt lúc đóng ca.
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS cash_shifts (
    id                BIGSERIAL PRIMARY KEY,
    code              VARCHAR(20)   NOT NULL,
    staff_id          BIGINT        REFERENCES staff (id),       -- thu ngân mở ca
    opened_at         TIMESTAMP     NOT NULL,
    opening_cash      NUMERIC(12,2) NOT NULL CHECK (opening_cash >= 0), -- tiền đầu ca
    closed_at         TIMESTAMP,
    closed_by         BIGINT        REFERENCES staff (id),
    order_count       INT,
    cash_sales        NUMERIC(12,2),
    transfer_sales    NUMERIC(12,2),
    card_sales        NUMERIC(12,2),
    ewallet_sales     NUMERIC(12,2),
    cash_in           NUMERIC(12,2),                             -- thu khác trong ca
    cash_out          NUMERIC(12,2),                             -- chi trong ca
    expected_cash     NUMERIC(12,2),                             -- tiền mặt lý thuyết
    counted_cash      NUMERIC(12,2),                             -- tiền mặt thực đếm
    denominations     VARCHAR(255),                              -- mệnh giá:số tờ, …
    difference_reason VARCHAR(255),
    next_opening_cash NUMERIC(12,2),                             -- để lại ca sau
    handover_cash     NUMERIC(12,2),                             -- nộp quản lý / chủ quán
    received_by       VARCHAR(100),
    note              VARCHAR(255),
    created_by VARCHAR(50)  NOT NULL DEFAULT 'system',
    created_at TIMESTAMP    NOT NULL DEFAULT now(),
    updated_by VARCHAR(50),
    updated_at TIMESTAMP,
    del_flag   BOOLEAN      NOT NULL DEFAULT FALSE,
    del_user   VARCHAR(50)
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_cash_shifts_code ON cash_shifts (code) WHERE NOT del_flag;
CREATE UNIQUE INDEX IF NOT EXISTS uq_cash_shifts_open ON cash_shifts ((closed_at IS NULL)) WHERE closed_at IS NULL AND NOT del_flag;

CREATE TABLE IF NOT EXISTS cash_movements (
    id         BIGSERIAL PRIMARY KEY,
    shift_id   BIGINT        NOT NULL REFERENCES cash_shifts (id),
    type       VARCHAR(3)    NOT NULL CHECK (type IN ('IN', 'OUT')),
    amount     NUMERIC(12,2) NOT NULL CHECK (amount > 0),
    reason     VARCHAR(255)  NOT NULL,
    staff_id   BIGINT        REFERENCES staff (id),
    created_by VARCHAR(50)  NOT NULL DEFAULT 'system',
    created_at TIMESTAMP    NOT NULL DEFAULT now(),
    updated_by VARCHAR(50),
    updated_at TIMESTAMP,
    del_flag   BOOLEAN      NOT NULL DEFAULT FALSE,
    del_user   VARCHAR(50)
);
CREATE INDEX IF NOT EXISTS idx_cash_movements_shift ON cash_movements (shift_id);

-- ---------------------------------------------------------------------
-- Phân ca & chấm công. Ca gãy = 2 ca trong ngày. Đi muộn / về sớm chỉ ghi nhận, không phạt tiền (BLLĐ Điều 127).
-- leave_type: P = nghỉ có phép, KL = nghỉ không lương.
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS work_shifts (
    id            BIGSERIAL PRIMARY KEY,
    name          VARCHAR(50) NOT NULL,
    start_time    TIME        NOT NULL,
    end_time      TIME        NOT NULL,
    grace_minutes INT         NOT NULL DEFAULT 5 CHECK (grace_minutes BETWEEN 0 AND 60),
    color         VARCHAR(20),
    active        BOOLEAN     NOT NULL DEFAULT TRUE,
    created_by VARCHAR(50)  NOT NULL DEFAULT 'system',
    created_at TIMESTAMP    NOT NULL DEFAULT now(),
    updated_by VARCHAR(50),
    updated_at TIMESTAMP,
    del_flag   BOOLEAN      NOT NULL DEFAULT FALSE,
    del_user   VARCHAR(50)
);

CREATE TABLE IF NOT EXISTS shift_assignments (
    id            BIGSERIAL PRIMARY KEY,
    staff_id      BIGINT      NOT NULL REFERENCES staff (id),
    work_shift_id BIGINT      NOT NULL REFERENCES work_shifts (id),
    work_date     DATE        NOT NULL,
    position      VARCHAR(30),                                  -- pha chế, thu ngân, phục vụ
    check_in_at   TIMESTAMP,
    check_out_at  TIMESTAMP,
    leave_type    VARCHAR(5)  CHECK (leave_type IN ('P', 'KL')),
    note          VARCHAR(255),
    adjusted_by   VARCHAR(50),                                  -- quản lý sửa giờ chấm công
    created_by VARCHAR(50)  NOT NULL DEFAULT 'system',
    created_at TIMESTAMP    NOT NULL DEFAULT now(),
    updated_by VARCHAR(50),
    updated_at TIMESTAMP,
    del_flag   BOOLEAN      NOT NULL DEFAULT FALSE,
    del_user   VARCHAR(50)
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_shift_assignments ON shift_assignments (staff_id, work_date, work_shift_id) WHERE NOT del_flag;
CREATE INDEX IF NOT EXISTS idx_shift_assignments_date ON shift_assignments (work_date);

-- Nhật ký thao tác (ghi theo lô từ hàng đợi; tự xóa sau app.audit.retention-days)
CREATE TABLE IF NOT EXISTS audit_logs (
    id          BIGSERIAL PRIMARY KEY,
    username    VARCHAR(50),
    module      VARCHAR(50)  NOT NULL,
    action      VARCHAR(40)  NOT NULL,
    method      VARCHAR(10)  NOT NULL,
    path        VARCHAR(255) NOT NULL,
    entity_id   VARCHAR(40),
    status_code INT          NOT NULL,
    duration_ms BIGINT       NOT NULL,
    ip          VARCHAR(64),
    detail      TEXT,
    created_at  TIMESTAMP    NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_audit_logs_created ON audit_logs (created_at);
CREATE INDEX IF NOT EXISTS idx_audit_logs_user ON audit_logs (username, created_at);

-- ---------------------------------------------------------------------
-- Thanh toán online qua VNPAY: 1 dòng mỗi lần tạo link (PAY) hoặc hoàn tiền (REFUND); kết quả IPN / querydr để đối soát.
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS payment_transactions (
    id                 BIGSERIAL PRIMARY KEY,
    payment_id         BIGINT        NOT NULL REFERENCES payments (id),
    provider           VARCHAR(20)   NOT NULL,                  -- VNPAY
    type               VARCHAR(10)   NOT NULL CHECK (type IN ('PAY', 'REFUND')),
    txn_ref            VARCHAR(40)   NOT NULL UNIQUE,           -- vnp_TxnRef
    amount             NUMERIC(12,2) NOT NULL,
    status             VARCHAR(20)   NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'SUCCESS', 'FAILED', 'EXPIRED')),
    bank_code          VARCHAR(20),                             -- VNPAYQR, VNBANK, INTCARD, NCB…
    provider_txn_no    VARCHAR(40),                             -- vnp_TransactionNo
    response_code      VARCHAR(10),                             -- vnp_ResponseCode
    transaction_status VARCHAR(10),                             -- vnp_TransactionStatus
    pay_url            TEXT,
    gateway_date       VARCHAR(14),                             -- vnp_CreateDate (yyyyMMddHHmmss, GMT+7)
    expire_at          TIMESTAMP,
    completed_at       TIMESTAMP,
    raw_response       TEXT,
    ipn_count          INT           NOT NULL DEFAULT 0,
    reconciled         BOOLEAN       NOT NULL DEFAULT FALSE,    -- đã đối soát
    reconciled_by      VARCHAR(50),
    reconciled_at      TIMESTAMP,
    reconcile_note     VARCHAR(255),
    created_at         TIMESTAMP     NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_payment_transactions_payment ON payment_transactions (payment_id);
CREATE INDEX IF NOT EXISTS idx_payment_transactions_created ON payment_transactions (created_at);

-- ---------------------------------------------------------------------
-- Phí giao hàng theo khu vực + đặt xe qua Ahamove (báo giá, tạo đơn, webhook trạng thái, hủy chuyến)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS delivery_zones (
    id                  BIGSERIAL PRIMARY KEY,
    name                VARCHAR(100)  NOT NULL,
    districts           VARCHAR(500)  NOT NULL,                -- quận / phường, cách nhau dấu phẩy
    fee                 NUMERIC(12,2) NOT NULL CHECK (fee >= 0),
    free_ship_threshold NUMERIC(12,2) CHECK (free_ship_threshold >= 0), -- miễn phí ship từ
    min_order_amount    NUMERIC(12,2) CHECK (min_order_amount >= 0),    -- đơn tối thiểu để giao
    estimated_minutes   INT,
    active              BOOLEAN       NOT NULL DEFAULT TRUE,
    sort_order          INT           NOT NULL DEFAULT 0,
    created_by VARCHAR(50)  NOT NULL DEFAULT 'system',
    created_at TIMESTAMP    NOT NULL DEFAULT now(),
    updated_by VARCHAR(50),
    updated_at TIMESTAMP,
    del_flag   BOOLEAN      NOT NULL DEFAULT FALSE,
    del_user   VARCHAR(50)
);

ALTER TABLE shipments ADD COLUMN IF NOT EXISTS district         VARCHAR(60);
ALTER TABLE shipments ADD COLUMN IF NOT EXISTS zone_name        VARCHAR(100);
ALTER TABLE shipments ADD COLUMN IF NOT EXISTS partner          VARCHAR(20);   -- AHAMOVE; NULL = gọi xe ngoài
ALTER TABLE shipments ADD COLUMN IF NOT EXISTS partner_order_id VARCHAR(60);
ALTER TABLE shipments ADD COLUMN IF NOT EXISTS partner_status   VARCHAR(30);
ALTER TABLE shipments ADD COLUMN IF NOT EXISTS partner_fee      NUMERIC(12,2); -- phí ship quán trả đối tác
ALTER TABLE shipments ADD COLUMN IF NOT EXISTS tracking_url     VARCHAR(255);
ALTER TABLE shipments ADD COLUMN IF NOT EXISTS driver_name      VARCHAR(100);
ALTER TABLE shipments ADD COLUMN IF NOT EXISTS driver_phone     VARCHAR(20);
ALTER TABLE shipments ADD COLUMN IF NOT EXISTS distance_km      NUMERIC(6,2);
CREATE INDEX IF NOT EXISTS idx_shipments_partner_order ON shipments (partner_order_id);

-- Lịch sử chuyến giao (nhân viên cập nhật / webhook đối tác)
CREATE TABLE IF NOT EXISTS shipment_events (
    id             BIGSERIAL PRIMARY KEY,
    shipment_id    BIGINT      NOT NULL REFERENCES shipments (id),
    source         VARCHAR(10) NOT NULL CHECK (source IN ('STAFF', 'PARTNER', 'SYSTEM')),
    status         VARCHAR(20),
    partner_status VARCHAR(30),
    note           VARCHAR(255),
    raw            TEXT,
    created_by     VARCHAR(50),
    created_at     TIMESTAMP   NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_shipment_events_shipment ON shipment_events (shipment_id);

-- ---------------------------------------------------------------------
-- Hóa đơn điện tử khởi tạo từ máy tính tiền (NĐ 254/2026, TT 91/2026): 1 hóa đơn / đơn hoàn tất, phát hành qua hàng đợi.
-- Không hủy: sai thì lập hóa đơn thay thế (REPLACEMENT, gốc → REPLACED) hoặc điều chỉnh (ADJUSTMENT).
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS invoices (
    id                  BIGSERIAL PRIMARY KEY,
    order_id            BIGINT        NOT NULL REFERENCES orders (id),
    kind                VARCHAR(15)   NOT NULL CHECK (kind IN ('ORIGINAL', 'REPLACEMENT', 'ADJUSTMENT')),
    related_invoice_id  BIGINT        REFERENCES invoices (id),
    status              VARCHAR(15)   NOT NULL CHECK (status IN ('PENDING', 'ISSUED', 'FAILED', 'REPLACED')),
    template_code       VARCHAR(2)    NOT NULL,                  -- 1 = GTGT, 2 = bán hàng
    series              VARCHAR(10)   NOT NULL,                  -- ký hiệu, ký tự thứ 5 = M (máy tính tiền)
    invoice_no          VARCHAR(10),
    tax_authority_code  VARCHAR(40),                             -- mã của CQT
    lookup_code         VARCHAR(40),
    lookup_url          VARCHAR(255),
    provider_invoice_id VARCHAR(60),
    buyer_name          VARCHAR(150),
    buyer_company       VARCHAR(255),
    buyer_tax_code      VARCHAR(20),
    buyer_address       VARCHAR(255),
    buyer_email         VARCHAR(100),
    buyer_phone         VARCHAR(15),
    buyer_id_number     VARCHAR(20),                             -- số định danh cá nhân
    amount_before_tax   NUMERIC(12,2) NOT NULL,
    vat_rate            NUMERIC(5,2),                            -- NULL = hóa đơn bán hàng
    vat_amount          NUMERIC(12,2) NOT NULL,
    total_amount        NUMERIC(12,2) NOT NULL,
    payment_method      VARCHAR(30),
    reason              VARCHAR(255),
    attempts            INT           NOT NULL DEFAULT 0,
    last_error          VARCHAR(500),
    issued_at           TIMESTAMP,
    created_by          VARCHAR(50),
    created_at          TIMESTAMP     NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_invoices_order ON invoices (order_id);
CREATE INDEX IF NOT EXISTS idx_invoices_status ON invoices (status);
CREATE UNIQUE INDEX IF NOT EXISTS uq_invoices_number ON invoices (series, invoice_no) WHERE invoice_no IS NOT NULL;

CREATE TABLE IF NOT EXISTS invoice_lines (
    id         BIGSERIAL PRIMARY KEY,
    invoice_id BIGINT        NOT NULL REFERENCES invoices (id),
    line_no    INT           NOT NULL,
    name       VARCHAR(255)  NOT NULL,
    unit       VARCHAR(20)   NOT NULL,
    quantity   NUMERIC(12,2) NOT NULL,
    unit_price NUMERIC(12,2) NOT NULL,
    amount     NUMERIC(12,2) NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_invoice_lines_invoice ON invoice_lines (invoice_id);

-- ---------------------------------------------------------------------
-- Tin nhắn chăm sóc khách hàng theo đơn: Zalo ZNS, lỗi thì SMS brandname (qua eSMS). Không phải tin quảng cáo.
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS notification_templates (
    code            VARCHAR(30)  PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    enabled         BOOLEAN      NOT NULL DEFAULT TRUE,
    zns_template_id VARCHAR(20),                                -- mẫu ZNS đã được Zalo duyệt
    sms_content     VARCHAR(320) NOT NULL,
    sms_fallback    BOOLEAN      NOT NULL DEFAULT TRUE,
    updated_by      VARCHAR(50)
);
INSERT INTO notification_templates (code, name, enabled, sms_content) VALUES
 ('ORDER_CONFIRMED',  'Xác nhận đơn',            TRUE,  'Coffeeholic: don {order_code} ({amount}) da duoc xac nhan, dang pha che. Theo doi: {link}'),
 ('ORDER_READY',      'Đồ uống sẵn sàng',        TRUE,  'Coffeeholic: don {order_code} da san sang, moi {customer_name} den quay nhan.'),
 ('ORDER_DELIVERING', 'Đang giao hàng',          TRUE,  'Coffeeholic: don {order_code} dang duoc giao. Tai xe {driver}. Theo doi: {link}'),
 ('ORDER_COMPLETED',  'Hoàn tất đơn',            TRUE,  'Coffeeholic cam on {customer_name}! Don {order_code} da hoan tat. Hoa don dien tu: {link}'),
 ('ORDER_CANCELLED',  'Đơn bị hủy',              TRUE,  'Coffeeholic: don {order_code} da bi huy. Ly do: {reason}. Tien da tra (neu co) se duoc hoan lai.')
ON CONFLICT (code) DO NOTHING;

CREATE TABLE IF NOT EXISTS notification_messages (
    id            BIGSERIAL PRIMARY KEY,
    order_id      BIGINT       REFERENCES orders (id),
    template_code VARCHAR(30)  NOT NULL,
    phone         VARCHAR(15)  NOT NULL,                        -- 84xxxxxxxxx
    status        VARCHAR(15)  NOT NULL CHECK (status IN ('PENDING', 'SENT', 'DELIVERED', 'FAILED')),
    channel       VARCHAR(5),                                   -- ZNS / SMS
    request_id    VARCHAR(48)  NOT NULL UNIQUE,                 -- mã đơn + sự kiện: mỗi sự kiện gửi 1 lần
    params        TEXT,
    content       VARCHAR(320),
    attempts      INT          NOT NULL DEFAULT 0,
    provider_ref  VARCHAR(60),
    error         VARCHAR(500),
    sent_at       TIMESTAMP,
    created_at    TIMESTAMP    NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_notification_messages_created ON notification_messages (created_at);


-- ---------------------------------------------------------------------
-- Quyền màn hình (khớp Perm.java / src/lib/perm.ts); Quản lý có mọi quyền quản trị.
-- ---------------------------------------------------------------------
INSERT INTO permissions (code, name, path, module, description, sort_order)
SELECT v.code, v.name, v.path, 'core', v.description, v.sort_order FROM (VALUES
 ('stocktakes',       'Kiểm kê kho',           '/admin/stocktakes',       'Phiếu kiểm kê, cân bằng kho', 78),
 ('inventory-report', 'Báo cáo xuất nhập tồn', '/admin/inventory-report', 'Báo cáo xuất – nhập – tồn, chốt tồn theo ngày', 79),
 ('coffee-costs',     'Giá vốn món',           '/admin/coffee-costs',     'Giá vốn theo định lượng, tỷ lệ food cost', 69)
,('cash-shifts',     'Ca thu ngân',           '/admin/cash-shifts',      'Mở / chốt ca, thu chi trong ca, bàn giao ca', 22)
,('work-shifts',     'Phân ca & chấm công',   '/admin/work-shifts',      'Ca mẫu, xếp ca, sửa chấm công, bảng chấm công', 92)
,('audit-logs',      'Nhật ký thao tác',      '/admin/audit-logs',       'Ai làm gì, lúc nào (chỉ xem)', 96)
,('payment-reconciliation', 'Đối soát thanh toán', '/admin/payment-reconciliation', 'Giao dịch VNPAY, truy vấn lại, đánh dấu đã đối soát', 94)
,('delivery-zones',  'Khu vực giao hàng',     '/admin/delivery-zones',   'Quận giao được, phí ship, miễn phí từ, đơn tối thiểu', 33)
,('invoices',        'Hóa đơn điện tử',       '/admin/invoices',         'Hóa đơn từ máy tính tiền: tra cứu, thay thế, gửi lại', 23)
,('notifications',   'Tin nhắn khách hàng',   '/admin/notifications',    'Mẫu tin ZNS / SMS theo đơn, nhật ký gửi', 95)
) AS v(code, name, path, description, sort_order)
WHERE NOT EXISTS (SELECT 1 FROM permissions p WHERE p.code = v.code AND NOT p.del_flag);

-- Nhân viên: mở / chốt ca ở quầy, kiểm kê (quản lý duyệt cân bằng), xuất hóa đơn công ty cho khách
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON p.code IN ('cash-shifts', 'stocktakes', 'invoices') AND NOT p.del_flag
WHERE r.code = 'STAFF'
  AND NOT EXISTS (SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id AND NOT rp.del_flag);

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON p.module = 'core' AND NOT p.del_flag
WHERE r.code = 'ADMIN'
  AND NOT EXISTS (SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id AND NOT rp.del_flag);

COMMIT;
