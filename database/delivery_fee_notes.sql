-- Ghi chú cho khách khi phí giao hàng khác thường: theo option giá (khung giờ cao điểm…) và khi có phụ phí thời tiết.
-- Trống = câu mặc định của app-core. Chạy được nhiều lần.
BEGIN;
ALTER TABLE delivery_fee_plans ADD COLUMN IF NOT EXISTS note         VARCHAR(255);
ALTER TABLE delivery_settings  ADD COLUMN IF NOT EXISTS weather_note VARCHAR(255);
COMMIT;
