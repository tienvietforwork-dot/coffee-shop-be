-- =====================================================================
-- Coffeeholic – Database schema v2 (PostgreSQL)
-- Nguồn: DB01_DatabaseDiagram (tài liệu 1.2) + các điều chỉnh:
--   * Phân quyền user – role – permission (như MES/SMT): users, roles, permissions,
--     user_roles, role_permissions. Chỉ 3 role: ADMIN (Quản lý), STAFF (Nhân viên), CUSTOMER (Khách hàng).
--   * users liên kết staff (nhân viên) hoặc customers (khách có tài khoản).
--   * vouchers: loại voucher, chỉ có discount_value + min_order_value (bỏ conditions) + quantity (số lượt);
--     voucher_details: từng voucher đã phát (mã, khách, phát / hết hạn lúc nào).
--   * Không có nghiệp vụ phân công: orders.staff_id / incidents.handled_by
--     là nhân viên đã thao tác (tự lấy từ tài khoản đăng nhập).
--   * MỌI bảng có 6 cột audit: created_by, created_at, updated_by, updated_at, del_flag, del_user.
--     Xóa = xóa mềm (del_flag = TRUE, del_user = người xóa). Ràng buộc UNIQUE là unique index
--     có điều kiện "WHERE NOT del_flag" để tạo lại được bản ghi đã xóa.
--   * Bảng liên kết dùng khóa chính id + unique index (vì xóa mềm không xóa được dòng khỏi khóa ghép).
-- 48 bảng. Chạy trong 1 transaction.
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
    date_of_birth  DATE,
    gender         VARCHAR(20),
    description    TEXT,
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
    product_type  VARCHAR(20) NOT NULL DEFAULT 'DRINK' CHECK (product_type IN ('DRINK', 'FOOD', 'ADD_ON')),
    display_order INT NOT NULL DEFAULT 0,
    created_by VARCHAR(50)  NOT NULL DEFAULT 'system',
    created_at TIMESTAMP    NOT NULL DEFAULT now(),
    updated_by VARCHAR(50),
    updated_at TIMESTAMP,
    del_flag   BOOLEAN      NOT NULL DEFAULT FALSE,
    del_user   VARCHAR(50)
);
CREATE UNIQUE INDEX uq_categories_name ON categories (lower(name)) WHERE NOT del_flag;
CREATE INDEX idx_categories_product_type ON categories (product_type, del_flag);

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
    auto_sold_out BOOLEAN     NOT NULL DEFAULT FALSE,  -- hệ thống tự "Hết món" vì thiếu nguyên liệu (xem material_quantity.sql)
    created_by VARCHAR(50)  NOT NULL DEFAULT 'system',
    created_at TIMESTAMP    NOT NULL DEFAULT now(),
    updated_by VARCHAR(50),
    updated_at TIMESTAMP,
    del_flag   BOOLEAN      NOT NULL DEFAULT FALSE,
    del_user   VARCHAR(50)
);
CREATE INDEX idx_coffees_category ON coffees (category_id);
CREATE INDEX idx_coffees_image ON coffees (image_id);

-- Nguyên liệu: tên + đơn vị + số lượng (cột quantity thêm ở cuối file); định lượng nằm ở recipe_materials
CREATE TABLE materials (
    id             BIGSERIAL PRIMARY KEY,
    name           VARCHAR(100)  NOT NULL,
    unit           VARCHAR(20)   NOT NULL,
    created_by VARCHAR(50)  NOT NULL DEFAULT 'system',
    created_at TIMESTAMP    NOT NULL DEFAULT now(),
    updated_by VARCHAR(50),
    updated_at TIMESTAMP,
    del_flag   BOOLEAN      NOT NULL DEFAULT FALSE,
    del_user   VARCHAR(50)
);
CREATE UNIQUE INDEX uq_materials_name ON materials (lower(name)) WHERE NOT del_flag;

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
    group_type      VARCHAR(20)  NOT NULL DEFAULT 'MANUAL' CHECK (group_type IN ('AUTOMATIC', 'MANUAL')),
    -- Example: {"min_spend": 5000000, "min_orders": 10, "last_order_days": 30}
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
    is_excluded BOOLEAN   NOT NULL DEFAULT FALSE,
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
    occurred_at TIMESTAMP NOT NULL DEFAULT now(),
    note        TEXT,
    outcome     TEXT,
    visible_to_customer BOOLEAN NOT NULL DEFAULT FALSE,
    staff_id    BIGINT REFERENCES staff (id),
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
    staff_id     BIGINT      REFERENCES staff (id),
    topic        VARCHAR(50),
    issue_type   VARCHAR(20) CHECK (issue_type IN ('PRODUCT', 'SERVICE', 'DELIVERY', 'PAYMENT', 'OTHER')),
    category     VARCHAR(30) CHECK (category IN ('COMPLAINT', 'SUGGESTION', 'PRAISE')),
    sentiment    VARCHAR(20) CHECK (sentiment IN ('POSITIVE', 'NEUTRAL', 'NEGATIVE')),
    content      TEXT        NOT NULL,
    status       VARCHAR(20) NOT NULL DEFAULT 'NEW'
                 CHECK (status IN ('NEW', 'IN_PROGRESS', 'ESCALATED', 'RESOLVED', 'CLOSED')),
    escalated    BOOLEAN     NOT NULL DEFAULT FALSE,
    resolution   TEXT,
    satisfaction VARCHAR(20) CHECK (satisfaction IN ('SATISFIED', 'UNSATISFIED')),
    rating       SMALLINT CHECK (rating BETWEEN 1 AND 5),
    closed_at    TIMESTAMP,
    created_by VARCHAR(50)  NOT NULL DEFAULT 'system',
    created_at TIMESTAMP    NOT NULL DEFAULT now(),
    updated_by VARCHAR(50),
    updated_at TIMESTAMP,
    del_flag   BOOLEAN      NOT NULL DEFAULT FALSE,
    del_user   VARCHAR(50)
);
CREATE INDEX idx_feedbacks_customer ON feedbacks (customer_id);

CREATE TABLE feedback_topics (
    feedback_id BIGINT NOT NULL REFERENCES feedbacks (id),
    topic       VARCHAR(100) NOT NULL,
    PRIMARY KEY (feedback_id, topic)
);
INSERT INTO feedback_topics (feedback_id, topic)
SELECT id, trim(topic) FROM feedbacks WHERE topic IS NOT NULL AND trim(topic) <> ''
ON CONFLICT DO NOTHING;

CREATE TABLE feedback_responses (
    id          BIGSERIAL PRIMARY KEY,
    feedback_id BIGINT NOT NULL REFERENCES feedbacks (id),
    staff_id    BIGINT REFERENCES staff (id),
    message     TEXT NOT NULL,
    sent_at     TIMESTAMP NOT NULL DEFAULT now(),
    read_at     TIMESTAMP,
    created_by  VARCHAR(50) NOT NULL DEFAULT 'system',
    created_at  TIMESTAMP NOT NULL DEFAULT now(),
    updated_by  VARCHAR(50),
    updated_at  TIMESTAMP,
    del_flag    BOOLEAN NOT NULL DEFAULT FALSE,
    del_user    VARCHAR(50)
);
CREATE INDEX idx_feedback_responses_feedback ON feedback_responses (feedback_id, sent_at);

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
    discount_type    VARCHAR(20)   NOT NULL DEFAULT 'PERCENTAGE'
                                      CHECK (discount_type IN ('PERCENTAGE', 'FIXED_AMOUNT', 'SPECIAL')),
    discount_percent NUMERIC(12,2),
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
    CHECK (end_date > start_date),
    CHECK (
        (discount_type = 'PERCENTAGE' AND (
            (apply_scope = 'ORDER' AND discount_percent IS NOT NULL AND discount_percent > 0 AND discount_percent <= 100)
            OR (apply_scope = 'COFFEE' AND (discount_percent IS NULL OR (discount_percent > 0 AND discount_percent <= 100)))
        ))
        OR (discount_type = 'FIXED_AMOUNT' AND (
            (apply_scope = 'ORDER' AND discount_percent IS NOT NULL AND discount_percent > 0)
            OR (apply_scope = 'COFFEE' AND (discount_percent IS NULL OR discount_percent > 0))
        ))
        OR (discount_type = 'SPECIAL' AND apply_scope = 'COFFEE'
            AND (discount_percent IS NULL OR discount_percent > 0))
    )
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

-- Lịch sử chuyển trạng thái đơn hàng: mỗi lần đổi trạng thái 1 dòng (from_status NULL = lúc đặt đơn).
CREATE TABLE order_status_history (
    id          BIGSERIAL PRIMARY KEY,
    order_id    BIGINT      NOT NULL REFERENCES orders (id),
    from_status VARCHAR(20),
    to_status   VARCHAR(20) NOT NULL,
    staff_id    BIGINT      REFERENCES staff (id),
    note        VARCHAR(255),
    created_by VARCHAR(50)  NOT NULL DEFAULT 'system',
    created_at TIMESTAMP    NOT NULL DEFAULT now(),
    updated_by VARCHAR(50),
    updated_at TIMESTAMP,
    del_flag   BOOLEAN      NOT NULL DEFAULT FALSE,
    del_user   VARCHAR(50)
);
CREATE INDEX idx_order_status_history_order ON order_status_history (order_id);

-- Loại voucher; quantity = số lượt được dùng tối đa. Voucher đã phát (ISSUED, còn hạn) giữ 1 lượt,
-- USED chiếm luôn, EXPIRED / REVOKED / quá hạn trả lượt. Còn lại = quantity − (đang giữ + đã dùng).
CREATE TABLE vouchers (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(100)  NOT NULL,
    discount_value  NUMERIC(12,2) NOT NULL CHECK (discount_value > 0),
    min_order_value NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (min_order_value >= 0),
    quantity        INT           NOT NULL CHECK (quantity > 0),   -- số lượt được dùng tối đa
    valid_days      INT           CHECK (valid_days > 0),          -- hạn mặc định khi phát (ngày)
    status          VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE')),
    created_by VARCHAR(50)  NOT NULL DEFAULT 'system',
    created_at TIMESTAMP    NOT NULL DEFAULT now(),
    updated_by VARCHAR(50),
    updated_at TIMESTAMP,
    del_flag   BOOLEAN      NOT NULL DEFAULT FALSE,
    del_user   VARCHAR(50)
);

-- Từng voucher đã phát: mã riêng, phát lúc nào, hết hạn lúc nào.
CREATE TABLE voucher_details (
    id              BIGSERIAL PRIMARY KEY,
    voucher_id      BIGINT        NOT NULL REFERENCES vouchers (id),
    code            VARCHAR(30)   NOT NULL,
    customer_id     BIGINT        REFERENCES customers (id),   -- null = voucher dùng chung
    order_id        BIGINT        REFERENCES orders (id),      -- đơn đã dùng voucher
    incident_id     BIGINT        REFERENCES incidents (id),   -- voucher đền bù sự cố
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
CREATE UNIQUE INDEX uq_voucher_details_code ON voucher_details (upper(code)) WHERE NOT del_flag;
CREATE INDEX idx_voucher_details_voucher ON voucher_details (voucher_id);
CREATE INDEX idx_voucher_details_customer ON voucher_details (customer_id);
CREATE INDEX idx_voucher_details_order ON voucher_details (order_id);
CREATE INDEX idx_voucher_details_incident ON voucher_details (incident_id);

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
    seller_name         VARCHAR(255),                            -- người bán lúc lập (thiết lập hóa đơn)
    seller_tax_code     VARCHAR(14),
    seller_address      VARCHAR(255),
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

-- Thiết lập hóa đơn (1 dòng): người bán, loại hóa đơn, ký hiệu = {mẫu số}C{YY}M{series_suffix} — xem invoice_settings.sql
CREATE TABLE IF NOT EXISTS invoice_settings (
    id              BIGINT PRIMARY KEY CHECK (id = 1),
    seller_name     VARCHAR(255)  NOT NULL,
    seller_tax_code VARCHAR(14)   NOT NULL,
    seller_address  VARCHAR(255),                          -- NULL = địa chỉ quán (delivery_settings.shop_address)
    vat_rate        NUMERIC(5,2)  CHECK (vat_rate BETWEEN 0 AND 10), -- NULL = hóa đơn bán hàng (mẫu 2)
    series_suffix   VARCHAR(2)    NOT NULL DEFAULT 'CH',
    tax_office_code VARCHAR(5),                            -- XXXXX trong mã CQT M1-YY-XXXXX-NNNNNNNNNNN
    lookup_url      VARCHAR(255),
    created_by VARCHAR(50)  NOT NULL DEFAULT 'system',
    created_at TIMESTAMP    NOT NULL DEFAULT now(),
    updated_by VARCHAR(50),
    updated_at TIMESTAMP,
    del_flag   BOOLEAN      NOT NULL DEFAULT FALSE,
    del_user   VARCHAR(50)
);

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


-- Giao hàng (quán tự giao): cửa hàng, miễn phí từ, đơn tối thiểu — phí theo khu vực ở cuối file (delivery_areas.sql)
CREATE TABLE IF NOT EXISTS delivery_settings (
    id                  BIGINT PRIMARY KEY CHECK (id = 1),     -- 1 dòng duy nhất
    enabled             BOOLEAN       NOT NULL DEFAULT TRUE,   -- false = tạm ngưng nhận đơn giao
    shop_address        VARCHAR(255)  NOT NULL,                -- điểm xuất phát của người giao
    shop_lat            DOUBLE PRECISION,
    shop_lng            DOUBLE PRECISION,
    free_ship_threshold NUMERIC(12,2) CHECK (free_ship_threshold >= 0), -- miễn phí ship từ
    min_order_amount    NUMERIC(12,2) CHECK (min_order_amount >= 0),    -- đơn tối thiểu để giao
    created_by VARCHAR(50)  NOT NULL DEFAULT 'system',
    created_at TIMESTAMP    NOT NULL DEFAULT now(),
    updated_by VARCHAR(50),
    updated_at TIMESTAMP,
    del_flag   BOOLEAN      NOT NULL DEFAULT FALSE,
    del_user   VARCHAR(50)
);

-- Giờ mở cửa — xem delivery_time_slots.sql
ALTER TABLE delivery_settings ADD COLUMN IF NOT EXISTS open_time  VARCHAR(5);   -- 'HH:mm' giờ VN; NULL = mở cả ngày
ALTER TABLE delivery_settings ADD COLUMN IF NOT EXISTS close_time VARCHAR(5);   -- < open_time = đóng sau nửa đêm

-- Ước tính giờ giao — xem delivery_eta.sql
ALTER TABLE delivery_settings ADD COLUMN IF NOT EXISTS speed_kmh NUMERIC(5,1) CHECK (speed_kmh > 0);

-- Mã sản phẩm, giá niêm yết từng dòng đơn, voucher trên đơn — xem product_code_order_discount.sql
ALTER TABLE coffees     ADD COLUMN IF NOT EXISTS code             VARCHAR(20);
CREATE UNIQUE INDEX IF NOT EXISTS uq_coffees_code ON coffees (upper(code)) WHERE NOT del_flag;
ALTER TABLE order_items ADD COLUMN IF NOT EXISTS list_price       NUMERIC(12,2) CHECK (list_price >= 0);
ALTER TABLE orders      ADD COLUMN IF NOT EXISTS voucher_code     VARCHAR(30);
ALTER TABLE orders      ADD COLUMN IF NOT EXISTS voucher_discount NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (voucher_discount >= 0);

-- Số lượng nguyên liệu, tự trừ theo đơn, tự hết món — xem material_quantity.sql
ALTER TABLE materials ADD COLUMN IF NOT EXISTS quantity       NUMERIC(12,2) NOT NULL DEFAULT 0;
ALTER TABLE orders    ADD COLUMN IF NOT EXISTS stock_deducted BOOLEAN       NOT NULL DEFAULT FALSE;

-- Phí giao hàng theo khu vực (tỉnh + phường), miễn phí cho nhóm khách — xem delivery_areas.sql
CREATE TABLE IF NOT EXISTS delivery_provinces (
    id     BIGSERIAL PRIMARY KEY,
    name   VARCHAR(100)  NOT NULL,                       -- "Thành phố Hà Nội" (danh mục 34 tỉnh / thành)
    fee    NUMERIC(12,2) NOT NULL CHECK (fee >= 0),      -- phí ship mặc định cho mọi phường / xã của tỉnh
    active BOOLEAN       NOT NULL DEFAULT TRUE           -- false = tạm ngưng giao tới tỉnh này
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_delivery_provinces_name ON delivery_provinces (lower(name));

-- phường / xã khác tỉnh: phí riêng (fee) hoặc không giao (blocked)
CREATE TABLE IF NOT EXISTS delivery_wards (
    id          BIGSERIAL PRIMARY KEY,
    province_id BIGINT        NOT NULL REFERENCES delivery_provinces (id) ON DELETE CASCADE,
    name        VARCHAR(100)  NOT NULL,
    fee         NUMERIC(12,2) CHECK (fee >= 0),          -- NULL = theo phí tỉnh
    blocked     BOOLEAN       NOT NULL DEFAULT FALSE
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_delivery_wards_name ON delivery_wards (province_id, lower(name));

-- nhóm khách CRM (customer_groups.id, phân cách dấu phẩy) được miễn phí giao hàng: Thân thiết, VIP…
ALTER TABLE delivery_settings ADD COLUMN IF NOT EXISTS free_ship_group_ids VARCHAR(255);

-- Người giao hàng: phân công, giờ nhận đồ — xem delivery_shippers.sql
ALTER TABLE shipments ADD COLUMN IF NOT EXISTS shipper_id   BIGINT REFERENCES staff (id);
ALTER TABLE shipments ADD COLUMN IF NOT EXISTS assigned_at  TIMESTAMP;
ALTER TABLE shipments ADD COLUMN IF NOT EXISTS picked_up_at TIMESTAMP;   -- lúc người giao nhận đồ, bắt đầu đi
CREATE INDEX IF NOT EXISTS idx_shipments_shipper ON shipments (shipper_id) WHERE NOT del_flag;

-- Gói giao định kỳ — xem delivery_subscriptions.sql
CREATE TABLE IF NOT EXISTS delivery_subscriptions (
    id            BIGSERIAL PRIMARY KEY,
    code          VARCHAR(20)  NOT NULL,
    customer_id   BIGINT       NOT NULL REFERENCES customers (id),
    address_id    BIGINT       NOT NULL REFERENCES customer_addresses (id),
    frequency     VARCHAR(10)  NOT NULL CHECK (frequency IN ('DAILY', 'WEEKLY')),
    days          VARCHAR(20),                                  -- WEEKLY: 1 (thứ 2) … 7 (CN), dấu phẩy
    delivery_time VARCHAR(5)   NOT NULL,                        -- 'HH:mm' giờ VN, giờ giao cố định
    start_date    DATE         NOT NULL,
    end_date      DATE,                                         -- NULL = không hạn
    status        VARCHAR(10)  NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'PAUSED', 'CANCELLED')),
    note          TEXT,
    last_run_date DATE,                                         -- ngày gần nhất đã tạo (thử tạo) đơn
    last_error    VARCHAR(255),                                 -- lỗi lần tạo đơn gần nhất
    created_by VARCHAR(50)  NOT NULL DEFAULT 'system',
    created_at TIMESTAMP    NOT NULL DEFAULT now(),
    updated_by VARCHAR(50),
    updated_at TIMESTAMP,
    del_flag   BOOLEAN      NOT NULL DEFAULT FALSE,
    del_user   VARCHAR(50)
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_delivery_subscriptions_code ON delivery_subscriptions (code) WHERE NOT del_flag;
CREATE INDEX IF NOT EXISTS idx_delivery_subscriptions_customer ON delivery_subscriptions (customer_id);

CREATE TABLE IF NOT EXISTS delivery_subscription_items (
    id              BIGSERIAL PRIMARY KEY,
    subscription_id BIGINT       NOT NULL REFERENCES delivery_subscriptions (id) ON DELETE CASCADE,
    coffee_id       BIGINT       NOT NULL REFERENCES coffees (id),
    quantity        INT          NOT NULL CHECK (quantity BETWEEN 1 AND 99),
    note            VARCHAR(255)
);

-- đơn tự tạo từ gói
ALTER TABLE orders ADD COLUMN IF NOT EXISTS subscription_id BIGINT REFERENCES delivery_subscriptions (id);
CREATE INDEX IF NOT EXISTS idx_orders_subscription ON orders (subscription_id) WHERE subscription_id IS NOT NULL;

-- Hoàn tiền tay khi cổng không hoàn được — xem refund_pending.sql
-- REFUND_PENDING: đã trả online, đơn bị từ chối / hủy, cổng không hoàn được → nhân viên chuyển khoản trả tay
ALTER TABLE payments DROP CONSTRAINT IF EXISTS payments_status_check;
ALTER TABLE payments ADD CONSTRAINT payments_status_check
    CHECK (status IN ('PENDING', 'PAID', 'FAILED', 'REFUNDED', 'REFUND_PENDING'));
ALTER TABLE payments ADD COLUMN IF NOT EXISTS refunded_at TIMESTAMP;
ALTER TABLE payments ADD COLUMN IF NOT EXISTS refund_ref  VARCHAR(100);   -- mã giao dịch hoàn (VNPAY / chuyển khoản tay)
ALTER TABLE payments ADD COLUMN IF NOT EXISTS refund_note TEXT;
