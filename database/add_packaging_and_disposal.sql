-- Quy cách đóng gói + phiếu hủy / xuất khác. PHẢI chạy trước khi deploy app-core có các cột này.
BEGIN;

-- Quy cách đóng gói của nguyên liệu: "Hộp 1 L" = 1000 (đơn vị gốc của nguyên liệu, vd ml).
CREATE TABLE IF NOT EXISTS material_packages (
    id          BIGSERIAL PRIMARY KEY,
    material_id BIGINT        NOT NULL REFERENCES materials (id),
    name        VARCHAR(50)   NOT NULL,
    size        NUMERIC(12,2) NOT NULL CHECK (size > 0),
    created_by VARCHAR(50)  NOT NULL DEFAULT 'system',
    created_at TIMESTAMP    NOT NULL DEFAULT now(),
    updated_by VARCHAR(50),
    updated_at TIMESTAMP,
    del_flag   BOOLEAN      NOT NULL DEFAULT FALSE,
    del_user   VARCHAR(50)
);
CREATE INDEX IF NOT EXISTS idx_material_packages_material ON material_packages (material_id);
CREATE UNIQUE INDEX IF NOT EXISTS uq_material_packages ON material_packages (material_id, lower(name)) WHERE NOT del_flag;

-- Lô ghi lại quy cách lúc nhập (chụp lại, không đổi khi quy cách bị sửa): 10 hộp × 500 ml.
ALTER TABLE material_batches ADD COLUMN IF NOT EXISTS package_name  VARCHAR(50);
ALTER TABLE material_batches ADD COLUMN IF NOT EXISTS package_size  NUMERIC(12,2) CHECK (package_size > 0);
ALTER TABLE material_batches ADD COLUMN IF NOT EXISTS package_count NUMERIC(12,2) CHECK (package_count > 0);

-- DISPOSAL = phiếu hủy (kể cả tự hủy lô hết hạn); EXPORT = xuất khác (trả nhà cung cấp, dùng nội bộ).
-- reason: EXPIRED, SPOILED, SPILLED, BREW_FAILED, OTHER_DISPOSAL, RETURN_SUPPLIER, INTERNAL_USE.
ALTER TABLE material_transactions ADD COLUMN IF NOT EXISTS reason VARCHAR(30);
ALTER TABLE material_transactions DROP CONSTRAINT IF EXISTS material_transactions_type_check;
ALTER TABLE material_transactions ADD CONSTRAINT material_transactions_type_check
    CHECK (type IN ('IMPORT', 'EXPORT', 'SALE', 'ADJUSTMENT', 'PRODUCTION_USE', 'PRODUCE', 'DISPOSAL'));

COMMIT;
