-- Số ngày cảnh báo trước khi lô hết hạn, riêng từng nguyên liệu.
-- NULL = mặc định: nguyên liệu thô 7 ngày (app.inventory.expiry-warning-days), bán thành phẩm không cảnh báo; 0 = không cảnh báo.
-- PHẢI chạy trước khi deploy app-core có cột này.
ALTER TABLE materials ADD COLUMN IF NOT EXISTS expiry_warning_days INT CHECK (expiry_warning_days >= 0);
