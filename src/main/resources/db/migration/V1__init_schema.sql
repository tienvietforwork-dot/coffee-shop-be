-- ============================================================================
-- Coffee Shop Management System - initial schema
-- Flyway runs this automatically on application startup against ANY empty
-- PostgreSQL database (local docker-compose, Neon, Supabase, Render, ...).
-- ============================================================================

CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name     VARCHAR(150),
    email         VARCHAR(150),
    phone         VARCHAR(30),
    role          VARCHAR(20)  NOT NULL DEFAULT 'STAFF', -- ADMIN / STAFF / SHIPPER
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE categories (
    id   BIGSERIAL PRIMARY KEY,
    name VARCHAR(150) NOT NULL
);

CREATE TABLE products (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(200)   NOT NULL,
    category_id BIGINT         REFERENCES categories(id),
    price       NUMERIC(12,2)  NOT NULL DEFAULT 0,
    image_url   VARCHAR(500),
    description VARCHAR(2000),
    active      BOOLEAN        NOT NULL DEFAULT TRUE
);

CREATE TABLE materials (
    id                  BIGSERIAL PRIMARY KEY,
    name                VARCHAR(150)  NOT NULL,
    unit                VARCHAR(20)   NOT NULL, -- kg / l / gram / ...
    quantity_in_stock   NUMERIC(14,3) NOT NULL DEFAULT 0,
    min_threshold       NUMERIC(14,3) NOT NULL DEFAULT 0,
    unit_price          NUMERIC(14,2) NOT NULL DEFAULT 0
);

CREATE TABLE product_materials (
    product_id        BIGINT        NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    material_id       BIGINT        NOT NULL REFERENCES materials(id) ON DELETE CASCADE,
    quantity_required NUMERIC(14,3) NOT NULL,
    PRIMARY KEY (product_id, material_id)
);

CREATE TABLE material_transactions (
    id          BIGSERIAL PRIMARY KEY,
    material_id BIGINT        NOT NULL REFERENCES materials(id),
    type        VARCHAR(10)   NOT NULL, -- IN / OUT / ADJUST
    quantity    NUMERIC(14,3) NOT NULL,
    reason      VARCHAR(500),
    created_by  BIGINT        REFERENCES users(id),
    created_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE orders (
    id            BIGSERIAL PRIMARY KEY,
    order_code    VARCHAR(30)   NOT NULL UNIQUE,
    customer_name VARCHAR(150),
    customer_phone VARCHAR(30),
    order_type    VARCHAR(20)   NOT NULL DEFAULT 'DINE_IN', -- DINE_IN / TAKE_AWAY / DELIVERY
    status        VARCHAR(20)   NOT NULL DEFAULT 'PENDING', -- PENDING/CONFIRMED/PREPARING/READY/COMPLETED/CANCELLED
    total_amount  NUMERIC(14,2) NOT NULL DEFAULT 0,
    created_by    BIGINT        REFERENCES users(id),
    created_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE order_items (
    id         BIGSERIAL PRIMARY KEY,
    order_id   BIGINT        NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id BIGINT        NOT NULL REFERENCES products(id),
    quantity   INTEGER       NOT NULL,
    unit_price NUMERIC(12,2) NOT NULL,
    subtotal   NUMERIC(14,2) NOT NULL
);

CREATE TABLE shipments (
    id          BIGSERIAL PRIMARY KEY,
    order_id    BIGINT        NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    shipper_id  BIGINT        REFERENCES users(id),
    address     VARCHAR(500),
    status      VARCHAR(20)   NOT NULL DEFAULT 'PENDING', -- PENDING/ASSIGNED/DELIVERING/DELIVERED/FAILED
    note        VARCHAR(1000),
    delivered_at TIMESTAMP
);

CREATE TABLE payments (
    id       BIGSERIAL PRIMARY KEY,
    order_id BIGINT        NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    method   VARCHAR(20)   NOT NULL DEFAULT 'CASH', -- CASH/BANK_TRANSFER/CARD
    amount   NUMERIC(14,2) NOT NULL,
    status   VARCHAR(20)   NOT NULL DEFAULT 'UNPAID', -- PAID/UNPAID
    paid_at  TIMESTAMP
);

CREATE TABLE daily_revenue (
    id            BIGSERIAL PRIMARY KEY,
    revenue_date  DATE          NOT NULL UNIQUE,
    total_revenue NUMERIC(16,2) NOT NULL DEFAULT 0,
    total_orders  INTEGER       NOT NULL DEFAULT 0,
    updated_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_products_category ON products(category_id);
CREATE INDEX idx_order_items_order ON order_items(order_id);
CREATE INDEX idx_order_items_product ON order_items(product_id);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_orders_created_at ON orders(created_at);
CREATE INDEX idx_material_transactions_material ON material_transactions(material_id);
CREATE INDEX idx_shipments_order ON shipments(order_id);
CREATE INDEX idx_payments_order ON payments(order_id);
