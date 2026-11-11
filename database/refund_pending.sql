-- Từ chối / hủy đơn khách đã chuyển khoản (VNPAY): hoàn tự động qua cổng; cổng từ chối thì khoản thanh toán chuyển
-- "chờ hoàn tiền" để nhân viên chuyển khoản trả tay và ghi mã giao dịch. Khách trả xong sau khi đơn đã bị từ chối
-- cũng được hoàn ngay.
-- Chạy tay trên Neon (an toàn khi chạy lại).
BEGIN;

-- REFUND_PENDING: đã trả online, đơn bị từ chối / hủy, cổng không hoàn được → nhân viên chuyển khoản trả tay
ALTER TABLE payments DROP CONSTRAINT IF EXISTS payments_status_check;
ALTER TABLE payments ADD CONSTRAINT payments_status_check
    CHECK (status IN ('PENDING', 'PAID', 'FAILED', 'REFUNDED', 'REFUND_PENDING'));
ALTER TABLE payments ADD COLUMN IF NOT EXISTS refunded_at TIMESTAMP;
ALTER TABLE payments ADD COLUMN IF NOT EXISTS refund_ref  VARCHAR(100);   -- mã giao dịch hoàn (VNPAY / chuyển khoản tay)
ALTER TABLE payments ADD COLUMN IF NOT EXISTS refund_note TEXT;

COMMIT;
