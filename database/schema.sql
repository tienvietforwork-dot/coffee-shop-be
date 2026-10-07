-- =====================================================================
-- Coffeeholic – Database schema v2 (PostgreSQL)
-- Nguồn: DB01_DatabaseDiagram (tài liệu 1.2) + các điều chỉnh:
--   * Phân quyền user – role – permission (như MES/SMT): users, roles, permissions,
--     user_roles, role_permissions. Chỉ 3 role: ADMIN (Quản lý), STAFF (Nhân viên), CUSTOMER (Khách hàng).
--   * users liên kết staff (nhân viên) hoặc customers (khách có tài khoản).
--   * vouchers: chỉ có discount_value + min_order_value (bỏ conditions).
--   * Không có nghiệp vụ phân công: orders.staff_id / incidents.handled_by / material_transactions.staff_id
--     là nhân viên đã thao tác (tự lấy từ tài khoản đăng nhập).
--   * MỌI bảng có 6 cột audit: created_by, created_at, updated_by, updated_at, del_flag, del_user.
--     Xóa = xóa mềm (del_flag = TRUE, del_user = người xóa). Ràng buộc UNIQUE là unique index
--     có điều kiện "WHERE NOT del_flag" để tạo lại được bản ghi đã xóa.
--   * Bảng liên kết dùng khóa chính id + unique index (vì xóa mềm không xóa được dòng khỏi khóa ghép).
-- 32 bảng. Chạy trong 1 transaction.
-- =====================================================================

BEGIN;

-- ---------------------------------------------------------------------
-- Nhân sự & khách hàng
-- ---------------------------------------------------------------------
CREATE TABLE staff (
    id          BIGSERIAL PRIMARY KEY,
    full_name   VARCHAR(100) NOT NULL,
    position    VARCHAR(50),
    phone       VARCHAR(15)  NOT NULL,
    email       VARCHAR(100),
    hire_date   DATE,
    work_status VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE' CHECK (work_status IN ('ACTIVE', 'ON_LEAVE', 'RESIGNED')),
    created_by VARCHAR(50)  NOT NULL DEFAULT 'system',
    created_at TIMESTAMP    NOT NULL DEFAULT now(),
    updated_by VARCHAR(50),
    updated_at TIMESTAMP,
    del_flag   BOOLEAN      NOT NULL DEFAULT FALSE,
    del_user   VARCHAR(50)
);
CREATE UNIQUE INDEX uq_staff_phone ON staff (phone) WHERE NOT del_flag;
CREATE UNIQUE INDEX uq_staff_email ON staff (email) WHERE NOT del_flag;

CREATE TABLE customers (
    id             BIGSERIAL PRIMARY KEY,
    full_name      VARCHAR(100),
    phone          VARCHAR(15) NOT NULL,
    email          VARCHAR(100),
    loyalty_points INT         NOT NULL DEFAULT 0 CHECK (loyalty_points >= 0),
    registered_at  TIMESTAMP   NOT NULL DEFAULT now(),
    created_by VARCHAR(50)  NOT NULL DEFAULT 'system',
    created_at TIMESTAMP    NOT NULL DEFAULT now(),
    updated_by VARCHAR(50),
    updated_at TIMESTAMP,
    del_flag   BOOLEAN      NOT NULL DEFAULT FALSE,
    del_user   VARCHAR(50)
);
CREATE UNIQUE INDEX uq_customers_phone ON customers (phone) WHERE NOT del_flag;

-- ---------------------------------------------------------------------
-- Tài khoản & phân quyền
-- ---------------------------------------------------------------------
CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL,              -- khách hàng: số điện thoại
    password_hash VARCHAR(255) NOT NULL,
    full_name     VARCHAR(150),
    email         VARCHAR(150),
    phone         VARCHAR(30),
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    staff_id      BIGINT       REFERENCES staff (id),
    customer_id   BIGINT       REFERENCES customers (id),
    last_login_at TIMESTAMP,
    created_by VARCHAR(50)  NOT NULL DEFAULT 'system',
    created_at TIMESTAMP    NOT NULL DEFAULT now(),
    updated_by VARCHAR(50),
    updated_at TIMESTAMP,
    del_flag   BOOLEAN      NOT NULL DEFAULT FALSE,
    del_user   VARCHAR(50)
);
CREATE UNIQUE INDEX uq_users_username ON users (username) WHERE NOT del_flag;
CREATE UNIQUE INDEX uq_users_staff ON users (staff_id) WHERE NOT del_flag AND staff_id IS NOT NULL;
CREATE UNIQUE INDEX uq_users_customer ON users (customer_id) WHERE NOT del_flag AND customer_id IS NOT NULL;

CREATE TABLE roles (
    id          BIGSERIAL PRIMARY KEY,
    code        VARCHAR(30)  NOT NULL CHECK (code IN ('ADMIN', 'STAFF', 'CUSTOMER')),
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_by VARCHAR(50)  NOT NULL DEFAULT 'system',
    created_at TIMESTAMP    NOT NULL DEFAULT now(),
    updated_by VARCHAR(50),
    updated_at TIMESTAMP,
    del_flag   BOOLEAN      NOT NULL DEFAULT FALSE,
    del_user   VARCHAR(50)
);
CREATE UNIQUE INDEX uq_roles_code ON roles (code) WHERE NOT del_flag;

-- Mỗi màn hình = 1 quyền (code + path), quyền thao tác đặc biệt có path NULL (vd. orders.refund)
CREATE TABLE permissions (
    id          BIGSERIAL PRIMARY KEY,
    code        VARCHAR(60)  NOT NULL,
    name        VARCHAR(150) NOT NULL,
    path        VARCHAR(100),
    module      VARCHAR(30)  NOT NULL CHECK (module IN ('core', 'crm', 'promotions', 'stats', 'system', 'customer')),
    description VARCHAR(255),
    sort_order  INT          NOT NULL DEFAULT 0,
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_by VARCHAR(50)  NOT NULL DEFAULT 'system',
    created_at TIMESTAMP    NOT NULL DEFAULT now(),
    updated_by VARCHAR(50),
    updated_at TIMESTAMP,
    del_flag   BOOLEAN      NOT NULL DEFAULT FALSE,
    del_user   VARCHAR(50)
);
CREATE UNIQUE INDEX uq_permissions_code ON permissions (code) WHERE NOT del_flag;

CREATE TABLE user_roles (
    id      BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users (id),
    role_id BIGINT NOT NULL REFERENCES roles (id),
    created_by VARCHAR(50)  NOT NULL DEFAULT 'system',
    created_at TIMESTAMP    NOT NULL DEFAULT now(),
    updated_by VARCHAR(50),
    updated_at TIMESTAMP,
    del_flag   BOOLEAN      NOT NULL DEFAULT FALSE,
    del_user   VARCHAR(50)
);
CREATE UNIQUE INDEX uq_user_roles ON user_roles (user_id, role_id) WHERE NOT del_flag;
CREATE INDEX idx_user_roles_role ON user_roles (role_id);

CREATE TABLE role_permissions (
    id            BIGSERIAL PRIMARY KEY,
    role_id       BIGINT NOT NULL REFERENCES roles (id),
    permission_id BIGINT NOT NULL REFERENCES permissions (id),
    created_by VARCHAR(50)  NOT NULL DEFAULT 'system',
    created_at TIMESTAMP    NOT NULL DEFAULT now(),
    updated_by VARCHAR(50),
    updated_at TIMESTAMP,
    del_flag   BOOLEAN      NOT NULL DEFAULT FALSE,
    del_user   VARCHAR(50)
);
CREATE UNIQUE INDEX uq_role_permissions ON role_permissions (role_id, permission_id) WHERE NOT del_flag;
CREATE INDEX idx_role_permissions_permission ON role_permissions (permission_id);

-- ---------------------------------------------------------------------
-- Cà phê & công thức
-- ---------------------------------------------------------------------
-- Ảnh tải lên (ảnh món…), lưu thẳng trong DB để không mất khi deploy lại; phục vụ qua /api/public/images/{id}
CREATE TABLE images (
    id           BIGSERIAL PRIMARY KEY,
    content_type VARCHAR(50) NOT NULL,
    size_bytes   INT         NOT NULL CHECK (size_bytes > 0),
    data         BYTEA       NOT NULL,
    created_by VARCHAR(50)  NOT NULL DEFAULT 'system',
    created_at TIMESTAMP    NOT NULL DEFAULT now(),
    updated_by VARCHAR(50),
    updated_at TIMESTAMP,
    del_flag   BOOLEAN      NOT NULL DEFAULT FALSE,
    del_user   VARCHAR(50)
);

CREATE TABLE categories (
    id            BIGSERIAL PRIMARY KEY,
    name          VARCHAR(100) NOT NULL,
    description   TEXT,
    display_order INT NOT NULL DEFAULT 0,
    created_by VARCHAR(50)  NOT NULL DEFAULT 'system',
    created_at TIMESTAMP    NOT NULL DEFAULT now(),
    updated_by VARCHAR(50),
    updated_at TIMESTAMP,
    del_flag   BOOLEAN      NOT NULL DEFAULT FALSE,
    del_user   VARCHAR(50)
);
CREATE UNIQUE INDEX uq_categories_name ON categories (lower(name)) WHERE NOT del_flag;

CREATE TABLE coffees (
    id          BIGSERIAL PRIMARY KEY,
    category_id BIGINT        NOT NULL REFERENCES categories (id),
    name        VARCHAR(150)  NOT NULL,
    image_id    BIGINT        REFERENCES images (id),  -- ảnh tải lên; image_url chỉ dùng cho link ngoài
    image_url   VARCHAR(500),
    price       NUMERIC(12,2) NOT NULL CHECK (price >= 0),
    description TEXT,
    status      VARCHAR(20)   NOT NULL DEFAULT 'AVAILABLE'
                CHECK (status IN ('AVAILABLE', 'SOLD_OUT', 'HIDDEN', 'DISCONTINUED')),
    auto_sold_out BOOLEAN     NOT NULL DEFAULT FALSE,  -- SOLD_OUT do hết nguyên liệu (tự mở bán lại khi đủ kho)
    created_by VARCHAR(50)  NOT NULL DEFAULT 'system',
    created_at TIMESTAMP    NOT NULL DEFAULT now(),
    updated_by VARCHAR(50),
    updated_at TIMESTAMP,
    del_flag   BOOLEAN      NOT NULL DEFAULT FALSE,
    del_user   VARCHAR(50)
);
CREATE INDEX idx_coffees_category ON coffees (category_id);
CREATE INDEX idx_coffees_image ON coffees (image_id);

-- Kho nguyên liệu (materials cần có trước recipe_materials)
CREATE TABLE materials (
    id             BIGSERIAL PRIMARY KEY,
    name           VARCHAR(100)  NOT NULL,
    unit           VARCHAR(20)   NOT NULL,
    stock_quantity NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (stock_quantity >= 0),
    min_stock      NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (min_stock >= 0),
    status         VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE')),
    -- RAW = nguyên liệu nhập mua; PREPARED = bán thành phẩm tự chế biến (vd cốt cold brew)
    kind            VARCHAR(20)   NOT NULL DEFAULT 'RAW' CHECK (kind IN ('RAW', 'PREPARED')),
    yield_quantity  NUMERIC(12,2) CHECK (yield_quantity > 0),   -- PREPARED: định lượng chuẩn (sản lượng 1 lần chế biến)
    prep_minutes       INT        CHECK (prep_minutes >= 0),       -- PREPARED: thời gian chế biến (phút)
    shelf_life_minutes INT        CHECK (shelf_life_minutes > 0),  -- PREPARED: hạn dùng tính từ lúc chế biến xong (phút)
    instructions       TEXT,                                       -- PREPARED: hướng dẫn chế biến định lượng chuẩn
    created_by VARCHAR(50)  NOT NULL DEFAULT 'system',
    created_at TIMESTAMP    NOT NULL DEFAULT now(),
    updated_by VARCHAR(50),
    updated_at TIMESTAMP,
    del_flag   BOOLEAN      NOT NULL DEFAULT FALSE,
    del_user   VARCHAR(50)
);
CREATE UNIQUE INDEX uq_materials_name ON materials (lower(name)) WHERE NOT del_flag;

-- Định lượng chuẩn của bán thành phẩm: material_id cần component_id với số lượng quantity
CREATE TABLE material_components (
    id           BIGSERIAL PRIMARY KEY,
    material_id  BIGINT        NOT NULL REFERENCES materials (id),
    component_id BIGINT        NOT NULL REFERENCES materials (id),
    quantity     NUMERIC(12,2) NOT NULL CHECK (quantity > 0),
    created_by VARCHAR(50)  NOT NULL DEFAULT 'system',
    created_at TIMESTAMP    NOT NULL DEFAULT now(),
    updated_by VARCHAR(50),
    updated_at TIMESTAMP,
    del_flag   BOOLEAN      NOT NULL DEFAULT FALSE,
    del_user   VARCHAR(50),
    CHECK (material_id <> component_id)
);
CREATE UNIQUE INDEX uq_material_components ON material_components (material_id, component_id) WHERE NOT del_flag;
CREATE INDEX idx_material_components_component ON material_components (component_id);

CREATE TABLE recipes (
    id            BIGSERIAL PRIMARY KEY,
    coffee_id     BIGINT  NOT NULL REFERENCES coffees (id),
    brew_method   VARCHAR(30) CHECK (brew_method IN ('PHIN', 'MACHINE', 'COLD_BREW', 'POUR_OVER')),
    version       INT     NOT NULL DEFAULT 1 CHECK (version > 0),
    description   TEXT,
    brew_time_min INT     CHECK (brew_time_min >= 0),
    is_active     BOOLEAN NOT NULL DEFAULT TRUE,
    created_by VARCHAR(50)  NOT NULL DEFAULT 'system',
    created_at TIMESTAMP    NOT NULL DEFAULT now(),
    updated_by VARCHAR(50),
    updated_at TIMESTAMP,
    del_flag   BOOLEAN      NOT NULL DEFAULT FALSE,
    del_user   VARCHAR(50)
);
CREATE UNIQUE INDEX uq_recipes_version ON recipes (coffee_id, version) WHERE NOT del_flag;
-- "1 công thức đang áp dụng" cho mỗi cà phê; các version cũ vẫn được lưu
CREATE UNIQUE INDEX uq_recipes_active_coffee ON recipes (coffee_id) WHERE is_active AND NOT del_flag;

CREATE TABLE recipe_materials (
    id          BIGSERIAL PRIMARY KEY,
    recipe_id   BIGINT        NOT NULL REFERENCES recipes (id),
    material_id BIGINT        NOT NULL REFERENCES materials (id),
    quantity    NUMERIC(10,2) NOT NULL CHECK (quantity > 0),
    note        VARCHAR(255),
    created_by VARCHAR(50)  NOT NULL DEFAULT 'system',
    created_at TIMESTAMP    NOT NULL DEFAULT now(),
    updated_by VARCHAR(50),
    updated_at TIMESTAMP,
    del_flag   BOOLEAN      NOT NULL DEFAULT FALSE,
    del_user   VARCHAR(50)
);
CREATE UNIQUE INDEX uq_recipe_materials ON recipe_materials (recipe_id, material_id) WHERE NOT del_flag;
CREATE INDEX idx_recipe_materials_material ON recipe_materials (material_id);

CREATE TABLE recipe_steps (
    id          BIGSERIAL PRIMARY KEY,
    recipe_id   BIGINT NOT NULL REFERENCES recipes (id),
    step_no     INT    NOT NULL CHECK (step_no > 0),
    instruction TEXT   NOT NULL,
    created_by VARCHAR(50)  NOT NULL DEFAULT 'system',
    created_at TIMESTAMP    NOT NULL DEFAULT now(),
    updated_by VARCHAR(50),
    updated_at TIMESTAMP,
    del_flag   BOOLEAN      NOT NULL DEFAULT FALSE,
    del_user   VARCHAR(50)
);
CREATE UNIQUE INDEX uq_recipe_steps ON recipe_steps (recipe_id, step_no) WHERE NOT del_flag;

CREATE TABLE material_batches (
    id                 BIGSERIAL PRIMARY KEY,
    material_id        BIGINT        NOT NULL REFERENCES materials (id),
    import_quantity    NUMERIC(12,2) NOT NULL CHECK (import_quantity > 0),
    remaining_quantity NUMERIC(12,2) NOT NULL CHECK (remaining_quantity >= 0),
    unit_cost          NUMERIC(12,2) CHECK (unit_cost >= 0),
    expiry_date        DATE,
    -- PREPARING = lô bán thành phẩm đang chế biến, chưa dùng được
    status             VARCHAR(20)   NOT NULL DEFAULT 'AVAILABLE' CHECK (status IN ('PREPARING', 'AVAILABLE', 'DEPLETED', 'EXPIRED')),
    ready_at           TIMESTAMP,                                   -- đang chế biến: dự kiến xong; xong: lúc hoàn tất
    expires_at         TIMESTAMP,                                   -- bán thành phẩm: hết hạn chính xác tới phút
    created_by VARCHAR(50)  NOT NULL DEFAULT 'system',
    created_at TIMESTAMP    NOT NULL DEFAULT now(),
    updated_by VARCHAR(50),
    updated_at TIMESTAMP,
    del_flag   BOOLEAN      NOT NULL DEFAULT FALSE,
    del_user   VARCHAR(50),
    CHECK (remaining_quantity <= import_quantity)
);
CREATE INDEX idx_material_batches_material ON material_batches (material_id);

-- ---------------------------------------------------------------------
-- Khách hàng (CRM)
-- ---------------------------------------------------------------------
CREATE TABLE customer_addresses (
    id              BIGSERIAL PRIMARY KEY,
    customer_id     BIGINT       NOT NULL REFERENCES customers (id),
    label           VARCHAR(30),                    -- nhà / công ty
    recipient_name  VARCHAR(100) NOT NULL,
    recipient_phone VARCHAR(15)  NOT NULL,
    address         VARCHAR(255) NOT NULL,
    is_default      BOOLEAN      NOT NULL DEFAULT FALSE,
    created_by VARCHAR(50)  NOT NULL DEFAULT 'system',
    created_at TIMESTAMP    NOT NULL DEFAULT now(),
    updated_by VARCHAR(50),
    updated_at TIMESTAMP,
    del_flag   BOOLEAN      NOT NULL DEFAULT FALSE,
    del_user   VARCHAR(50)
);
CREATE INDEX idx_customer_addresses_customer ON customer_addresses (customer_id);
CREATE UNIQUE INDEX uq_customer_addresses_default ON customer_addresses (customer_id) WHERE is_default AND NOT del_flag;

CREATE TABLE customer_groups (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    description     TEXT,
    criteria_type   VARCHAR(30) CHECK (criteria_type IN ('SPENDING', 'FREQUENCY', 'RECENCY', 'FAVORITE_COFFEE', 'MANUAL')),
    criteria_rule   JSONB,
    recalculated_at TIMESTAMP,
    created_by VARCHAR(50)  NOT NULL DEFAULT 'system',
    created_at TIMESTAMP    NOT NULL DEFAULT now(),
    updated_by VARCHAR(50),
    updated_at TIMESTAMP,
    del_flag   BOOLEAN      NOT NULL DEFAULT FALSE,
    del_user   VARCHAR(50)
);
CREATE UNIQUE INDEX uq_customer_groups_name ON customer_groups (lower(name)) WHERE NOT del_flag;

CREATE TABLE customer_group_members (
    id          BIGSERIAL PRIMARY KEY,
    group_id    BIGINT    NOT NULL REFERENCES customer_groups (id),
    customer_id BIGINT    NOT NULL REFERENCES customers (id),
    is_manual   BOOLEAN   NOT NULL DEFAULT FALSE,
    added_at    TIMESTAMP NOT NULL DEFAULT now(),
    created_by VARCHAR(50)  NOT NULL DEFAULT 'system',
    created_at TIMESTAMP    NOT NULL DEFAULT now(),
    updated_by VARCHAR(50),
    updated_at TIMESTAMP,
    del_flag   BOOLEAN      NOT NULL DEFAULT FALSE,
    del_user   VARCHAR(50)
);
CREATE UNIQUE INDEX uq_customer_group_members ON customer_group_members (group_id, customer_id) WHERE NOT del_flag;
CREATE INDEX idx_cgm_customer ON customer_group_members (customer_id);

CREATE TABLE interactions (
    id          BIGSERIAL PRIMARY KEY,
    customer_id BIGINT      NOT NULL REFERENCES customers (id),
    channel     VARCHAR(20) CHECK (channel IN ('CALL', 'CHAT', 'EMAIL', 'IN_STORE', 'NOTE')),
    content     TEXT,
    note        TEXT,
    status      VARCHAR(20) NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN', 'FOLLOW_UP', 'CLOSED')),
    remind_at   TIMESTAMP,
    closed_at   TIMESTAMP,
    created_by VARCHAR(50)  NOT NULL DEFAULT 'system',
    created_at TIMESTAMP    NOT NULL DEFAULT now(),
    updated_by VARCHAR(50),
    updated_at TIMESTAMP,
    del_flag   BOOLEAN      NOT NULL DEFAULT FALSE,
    del_user   VARCHAR(50)
);
CREATE INDEX idx_interactions_customer ON interactions (customer_id);
CREATE INDEX idx_interactions_remind ON interactions (remind_at) WHERE closed_at IS NULL AND NOT del_flag;

CREATE TABLE feedbacks (
    id           BIGSERIAL PRIMARY KEY,
    customer_id  BIGINT      NOT NULL REFERENCES customers (id),
    topic        VARCHAR(50),
    category     VARCHAR(30) CHECK (category IN ('COMPLAINT', 'SUGGESTION', 'PRAISE')),
    sentiment    VARCHAR(20) CHECK (sentiment IN ('POSITIVE', 'NEUTRAL', 'NEGATIVE')),
    content      TEXT        NOT NULL,
    status       VARCHAR(20) NOT NULL DEFAULT 'NEW'
                 CHECK (status IN ('NEW', 'IN_PROGRESS', 'ESCALATED', 'RESOLVED', 'CLOSED')),
    escalated    BOOLEAN     NOT NULL DEFAULT FALSE,
    resolution   TEXT,
    satisfaction VARCHAR(20) CHECK (satisfaction IN ('SATISFIED', 'UNSATISFIED')),
    closed_at    TIMESTAMP,
    created_by VARCHAR(50)  NOT NULL DEFAULT 'system',
    created_at TIMESTAMP    NOT NULL DEFAULT now(),
    updated_by VARCHAR(50),
    updated_at TIMESTAMP,
    del_flag   BOOLEAN      NOT NULL DEFAULT FALSE,
    del_user   VARCHAR(50)
);
CREATE INDEX idx_feedbacks_customer ON feedbacks (customer_id);

-- ---------------------------------------------------------------------
-- Bán hàng & vận hành
-- ---------------------------------------------------------------------
CREATE TABLE dining_tables (
    id       BIGSERIAL PRIMARY KEY,
    table_no VARCHAR(10)  NOT NULL,
    qr_code  VARCHAR(255) NOT NULL,
    area     VARCHAR(50),
    capacity INT          CHECK (capacity > 0),
    status   VARCHAR(20)  NOT NULL DEFAULT 'AVAILABLE' CHECK (status IN ('AVAILABLE', 'OCCUPIED', 'INACTIVE')),
    created_by VARCHAR(50)  NOT NULL DEFAULT 'system',
    created_at TIMESTAMP    NOT NULL DEFAULT now(),
    updated_by VARCHAR(50),
    updated_at TIMESTAMP,
    del_flag   BOOLEAN      NOT NULL DEFAULT FALSE,
    del_user   VARCHAR(50)
);
CREATE UNIQUE INDEX uq_dining_tables_no ON dining_tables (table_no) WHERE NOT del_flag;
CREATE UNIQUE INDEX uq_dining_tables_qr ON dining_tables (qr_code) WHERE NOT del_flag;

CREATE TABLE carts (
    id           BIGSERIAL PRIMARY KEY,
    session_code VARCHAR(64) NOT NULL UNIQUE,
    table_id     BIGINT      REFERENCES dining_tables (id),
    customer_id  BIGINT      REFERENCES customers (id),
    status       VARCHAR(20) NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN', 'ORDERED', 'ABANDONED')),
    created_by VARCHAR(50)  NOT NULL DEFAULT 'system',
    created_at TIMESTAMP    NOT NULL DEFAULT now(),
    updated_by VARCHAR(50),
    updated_at TIMESTAMP,
    del_flag   BOOLEAN      NOT NULL DEFAULT FALSE,
    del_user   VARCHAR(50)
);
CREATE INDEX idx_carts_table ON carts (table_id);
CREATE INDEX idx_carts_customer ON carts (customer_id);

CREATE TABLE cart_items (
    id        BIGSERIAL PRIMARY KEY,
    cart_id   BIGINT        NOT NULL REFERENCES carts (id),
    coffee_id BIGINT        NOT NULL REFERENCES coffees (id),
    quantity  INT           NOT NULL CHECK (quantity > 0),
    note      VARCHAR(255),
    subtotal  NUMERIC(12,2) NOT NULL CHECK (subtotal >= 0),
    created_by VARCHAR(50)  NOT NULL DEFAULT 'system',
    created_at TIMESTAMP    NOT NULL DEFAULT now(),
    updated_by VARCHAR(50),
    updated_at TIMESTAMP,
    del_flag   BOOLEAN      NOT NULL DEFAULT FALSE,
    del_user   VARCHAR(50)
);
CREATE INDEX idx_cart_items_cart ON cart_items (cart_id);
CREATE INDEX idx_cart_items_coffee ON cart_items (coffee_id);

-- ---------------------------------------------------------------------
-- Khuyến mãi
-- ---------------------------------------------------------------------
CREATE TABLE promotions (
    id               BIGSERIAL PRIMARY KEY,
    name             VARCHAR(150)  NOT NULL,
    description      TEXT,
    apply_scope      VARCHAR(10)   NOT NULL CHECK (apply_scope IN ('ORDER', 'COFFEE')),
    discount_percent NUMERIC(5,2)  CHECK (discount_percent > 0 AND discount_percent <= 100),
    min_order_amount NUMERIC(12,2) CHECK (min_order_amount >= 0),
    start_date       TIMESTAMP     NOT NULL,
    end_date         TIMESTAMP     NOT NULL,
    status           VARCHAR(20)   NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT', 'ACTIVE', 'PAUSED', 'ENDED')),
    created_by VARCHAR(50)  NOT NULL DEFAULT 'system',
    created_at TIMESTAMP    NOT NULL DEFAULT now(),
    updated_by VARCHAR(50),
    updated_at TIMESTAMP,
    del_flag   BOOLEAN      NOT NULL DEFAULT FALSE,
    del_user   VARCHAR(50),
    CHECK (end_date > start_date)
);
CREATE INDEX idx_promotions_period ON promotions (start_date, end_date);

CREATE TABLE promotion_items (
    id              BIGSERIAL PRIMARY KEY,
    promotion_id    BIGINT        NOT NULL REFERENCES promotions (id),
    coffee_id       BIGINT        NOT NULL REFERENCES coffees (id),
    custom_discount NUMERIC(12,2) CHECK (custom_discount >= 0),
    sale_price      NUMERIC(12,2) CHECK (sale_price >= 0),
    max_quantity    INT           CHECK (max_quantity > 0),
    sold_quantity   INT           NOT NULL DEFAULT 0 CHECK (sold_quantity >= 0),
    created_by VARCHAR(50)  NOT NULL DEFAULT 'system',
    created_at TIMESTAMP    NOT NULL DEFAULT now(),
    updated_by VARCHAR(50),
    updated_at TIMESTAMP,
    del_flag   BOOLEAN      NOT NULL DEFAULT FALSE,
    del_user   VARCHAR(50),
    CHECK (max_quantity IS NULL OR sold_quantity <= max_quantity)
);
CREATE UNIQUE INDEX uq_promotion_items ON promotion_items (promotion_id, coffee_id) WHERE NOT del_flag;
CREATE INDEX idx_promotion_items_coffee ON promotion_items (coffee_id);

-- ---------------------------------------------------------------------
-- Đơn hàng
-- ---------------------------------------------------------------------
CREATE TABLE orders (
    id              BIGSERIAL PRIMARY KEY,
    order_code      VARCHAR(30)   NOT NULL UNIQUE,
    customer_id     BIGINT        REFERENCES customers (id),
    staff_id        BIGINT        REFERENCES staff (id),       -- nhân viên đã xử lý gần nhất
    table_id        BIGINT        REFERENCES dining_tables (id),
    cart_id         BIGINT        UNIQUE REFERENCES carts (id),
    promotion_id    BIGINT        REFERENCES promotions (id),
    order_type      VARCHAR(20)   NOT NULL CHECK (order_type IN ('DINE_IN', 'TAKE_AWAY', 'DELIVERY')),
    channel         VARCHAR(20)   NOT NULL CHECK (channel IN ('COUNTER', 'QR', 'ONLINE')),
    status          VARCHAR(20)   NOT NULL DEFAULT 'PENDING' CHECK (status IN
                    ('PENDING', 'CONFIRMED', 'PREPARING', 'READY', 'DELIVERING', 'COMPLETED', 'REJECTED', 'CANCELLED')),
    subtotal        NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (subtotal >= 0),
    discount_amount NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (discount_amount >= 0),
    total_amount    NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (total_amount >= 0),
    note            TEXT,
    cancel_reason   TEXT,
    pickup_time     TIMESTAMP,
    ordered_at      TIMESTAMP     NOT NULL DEFAULT now(),
    created_by VARCHAR(50)  NOT NULL DEFAULT 'system',
    created_at TIMESTAMP    NOT NULL DEFAULT now(),
    updated_by VARCHAR(50),
    updated_at TIMESTAMP,
    del_flag   BOOLEAN      NOT NULL DEFAULT FALSE,
    del_user   VARCHAR(50)
);
CREATE INDEX idx_orders_customer ON orders (customer_id);
CREATE INDEX idx_orders_staff ON orders (staff_id);
CREATE INDEX idx_orders_table ON orders (table_id);
CREATE INDEX idx_orders_promotion ON orders (promotion_id);
CREATE INDEX idx_orders_status ON orders (status);
CREATE INDEX idx_orders_ordered_at ON orders (ordered_at);

CREATE TABLE order_items (
    id         BIGSERIAL PRIMARY KEY,
    order_id   BIGINT        NOT NULL REFERENCES orders (id),
    coffee_id  BIGINT        NOT NULL REFERENCES coffees (id),
    quantity   INT           NOT NULL CHECK (quantity > 0),
    unit_price NUMERIC(12,2) NOT NULL CHECK (unit_price >= 0),   -- giá thực thu (sau giảm theo cà phê)
    line_total NUMERIC(12,2) NOT NULL CHECK (line_total >= 0),
    note       VARCHAR(255),
    created_by VARCHAR(50)  NOT NULL DEFAULT 'system',
    created_at TIMESTAMP    NOT NULL DEFAULT now(),
    updated_by VARCHAR(50),
    updated_at TIMESTAMP,
    del_flag   BOOLEAN      NOT NULL DEFAULT FALSE,
    del_user   VARCHAR(50)
);
CREATE INDEX idx_order_items_order ON order_items (order_id);
CREATE INDEX idx_order_items_coffee ON order_items (coffee_id);

CREATE TABLE payments (
    id               BIGSERIAL PRIMARY KEY,
    order_id         BIGINT        NOT NULL REFERENCES orders (id),
    method           VARCHAR(20)   NOT NULL CHECK (method IN ('CASH', 'BANK_TRANSFER', 'E_WALLET', 'CARD')),
    amount           NUMERIC(12,2) NOT NULL CHECK (amount >= 0),
    status           VARCHAR(20)   NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'PAID', 'FAILED', 'REFUNDED')),
    transaction_code VARCHAR(100)  UNIQUE,
    paid_at          TIMESTAMP,
    refund_amount    NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (refund_amount >= 0),
    created_by VARCHAR(50)  NOT NULL DEFAULT 'system',
    created_at TIMESTAMP    NOT NULL DEFAULT now(),
    updated_by VARCHAR(50),
    updated_at TIMESTAMP,
    del_flag   BOOLEAN      NOT NULL DEFAULT FALSE,
    del_user   VARCHAR(50),
    CHECK (refund_amount <= amount)
);
CREATE INDEX idx_payments_order ON payments (order_id);

CREATE TABLE shipments (
    id                 BIGSERIAL PRIMARY KEY,
    order_id           BIGINT        NOT NULL UNIQUE REFERENCES orders (id),
    address_id         BIGINT        NOT NULL REFERENCES customer_addresses (id),
    carrier            VARCHAR(50),
    tracking_code      VARCHAR(100),
    shipping_fee       NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (shipping_fee >= 0),
    status             VARCHAR(20)   NOT NULL DEFAULT 'PENDING' CHECK (status IN
                       ('PENDING', 'BOOKED', 'DRIVER_ACCEPTED', 'DELIVERING', 'DELIVERED', 'FAILED')),
    booked_at          TIMESTAMP,
    driver_accepted_at TIMESTAMP,
    delivered_at       TIMESTAMP,
    note               TEXT,
    created_by VARCHAR(50)  NOT NULL DEFAULT 'system',
    created_at TIMESTAMP    NOT NULL DEFAULT now(),
    updated_by VARCHAR(50),
    updated_at TIMESTAMP,
    del_flag   BOOLEAN      NOT NULL DEFAULT FALSE,
    del_user   VARCHAR(50)
);
CREATE INDEX idx_shipments_address ON shipments (address_id);

CREATE TABLE incidents (
    id          BIGSERIAL PRIMARY KEY,
    order_id    BIGINT       NOT NULL REFERENCES orders (id),
    handled_by  BIGINT       REFERENCES staff (id),          -- nhân viên đã xử lý
    type        VARCHAR(30)  NOT NULL CHECK (type IN ('WRONG_ITEM', 'QUALITY', 'LATE', 'SPILLED', 'MISSING_ITEM', 'OTHER')),
    severity    VARCHAR(20)  CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH')),
    reason      VARCHAR(255),
    description TEXT,
    resolution  TEXT,
    status      VARCHAR(20)  NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN', 'RESOLVED')),
    reported_at TIMESTAMP    NOT NULL DEFAULT now(),
    resolved_at TIMESTAMP,
    created_by VARCHAR(50)  NOT NULL DEFAULT 'system',
    created_at TIMESTAMP    NOT NULL DEFAULT now(),
    updated_by VARCHAR(50),
    updated_at TIMESTAMP,
    del_flag   BOOLEAN      NOT NULL DEFAULT FALSE,
    del_user   VARCHAR(50)
);
CREATE INDEX idx_incidents_order ON incidents (order_id);

CREATE TABLE loyalty_point_history (
    id            BIGSERIAL PRIMARY KEY,
    customer_id   BIGINT       NOT NULL REFERENCES customers (id),
    order_id      BIGINT       REFERENCES orders (id),
    points_change INT          NOT NULL,
    reason        VARCHAR(100),
    created_by VARCHAR(50)  NOT NULL DEFAULT 'system',
    created_at TIMESTAMP    NOT NULL DEFAULT now(),
    updated_by VARCHAR(50),
    updated_at TIMESTAMP,
    del_flag   BOOLEAN      NOT NULL DEFAULT FALSE,
    del_user   VARCHAR(50)
);
CREATE INDEX idx_lph_customer ON loyalty_point_history (customer_id);
CREATE INDEX idx_lph_order ON loyalty_point_history (order_id);

CREATE TABLE vouchers (
    id              BIGSERIAL PRIMARY KEY,
    code            VARCHAR(30)   NOT NULL,
    customer_id     BIGINT        REFERENCES customers (id),   -- null = voucher dùng chung
    order_id        BIGINT        REFERENCES orders (id),      -- đơn đã dùng voucher
    incident_id     BIGINT        REFERENCES incidents (id),   -- voucher đền bù sự cố
    discount_value  NUMERIC(12,2) NOT NULL CHECK (discount_value > 0),
    min_order_value NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (min_order_value >= 0),
    status          VARCHAR(20)   NOT NULL DEFAULT 'ISSUED' CHECK (status IN ('ISSUED', 'USED', 'EXPIRED', 'REVOKED')),
    issued_at       TIMESTAMP     NOT NULL DEFAULT now(),
    expires_at      TIMESTAMP,
    created_by VARCHAR(50)  NOT NULL DEFAULT 'system',
    created_at TIMESTAMP    NOT NULL DEFAULT now(),
    updated_by VARCHAR(50),
    updated_at TIMESTAMP,
    del_flag   BOOLEAN      NOT NULL DEFAULT FALSE,
    del_user   VARCHAR(50),
    CHECK (expires_at IS NULL OR expires_at > issued_at)
);
CREATE UNIQUE INDEX uq_vouchers_code ON vouchers (upper(code)) WHERE NOT del_flag;
CREATE INDEX idx_vouchers_customer ON vouchers (customer_id);
CREATE INDEX idx_vouchers_order ON vouchers (order_id);
CREATE INDEX idx_vouchers_incident ON vouchers (incident_id);

CREATE TABLE material_transactions (
    id            BIGSERIAL PRIMARY KEY,
    batch_id      BIGINT        NOT NULL REFERENCES material_batches (id),
    staff_id      BIGINT        REFERENCES staff (id),         -- nhân viên đã thao tác
    order_item_id BIGINT        REFERENCES order_items (id),
    -- PRODUCTION_USE = nguyên liệu xuất để chế biến bán thành phẩm; PRODUCE = lô chế biến xong nhập kho
    type          VARCHAR(20)   NOT NULL CHECK (type IN ('IMPORT', 'EXPORT', 'SALE', 'ADJUSTMENT', 'PRODUCTION_USE', 'PRODUCE')),
    produced_batch_id BIGINT    REFERENCES material_batches (id), -- PRODUCTION_USE: dùng để chế biến lô bán thành phẩm nào
    quantity      NUMERIC(12,2) NOT NULL,                       -- dương = nhập, âm = xuất
    note          VARCHAR(255),
    created_by VARCHAR(50)  NOT NULL DEFAULT 'system',
    created_at TIMESTAMP    NOT NULL DEFAULT now(),
    updated_by VARCHAR(50),
    updated_at TIMESTAMP,
    del_flag   BOOLEAN      NOT NULL DEFAULT FALSE,
    del_user   VARCHAR(50)
);
CREATE INDEX idx_material_tx_batch ON material_transactions (batch_id);
CREATE INDEX idx_material_tx_staff ON material_transactions (staff_id);
CREATE INDEX idx_material_tx_order_item ON material_transactions (order_item_id);
CREATE INDEX idx_material_tx_produced_batch ON material_transactions (produced_batch_id);
CREATE INDEX idx_material_tx_created ON material_transactions (created_at);

-- ---------------------------------------------------------------------
-- Background service: mỗi lượt chạy 1 dòng (giống A_BACKGROUND_SERVICE bên MES)
-- created_by = 'Service' khi chạy theo chu kỳ, hoặc username của người làm thao tác kích hoạt
-- ---------------------------------------------------------------------
CREATE TABLE background_service_logs (
    id            BIGSERIAL PRIMARY KEY,
    service       VARCHAR(100) NOT NULL,
    check_time    TIMESTAMP    NOT NULL,
    duration_ms   INT,
    msg           TEXT,
    next_run_time TIMESTAMP,
    created_by VARCHAR(50)  NOT NULL DEFAULT 'system',
    created_at TIMESTAMP    NOT NULL DEFAULT now(),
    updated_by VARCHAR(50),
    updated_at TIMESTAMP,
    del_flag   BOOLEAN      NOT NULL DEFAULT FALSE,
    del_user   VARCHAR(50)
);
CREATE INDEX idx_background_service_logs ON background_service_logs (service, check_time DESC);

COMMIT;
