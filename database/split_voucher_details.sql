-- Tách voucher thành 2 bảng:
--   vouchers        = loại voucher: giá trị giảm, đơn tối thiểu, quantity = số lượt được dùng tối đa.
--   voucher_details = từng voucher đã phát: mã riêng, khách, phát lúc nào, hết hạn lúc nào, đơn đã dùng.
-- Giữ chỗ: mỗi voucher_details ISSUED còn hạn giữ 1 lượt, USED chiếm luôn; EXPIRED / REVOKED / quá hạn trả lượt.
--   Còn lại = quantity − (đang giữ + đã dùng); hết lượt thì không phát thêm.
-- Bảng vouchers cũ đổi tên thành voucher_details (giữ nguyên id, mã, FK), mỗi nhóm
-- (giá trị, đơn tối thiểu, có phải đền bù) thành 1 loại voucher.
-- Bản sao dữ liệu cũ: legacy_backup.vouchers_20261023.
-- PHẢI chạy trước khi deploy app-core có VoucherDetail.

BEGIN;

CREATE SCHEMA IF NOT EXISTS legacy_backup;
CREATE TABLE legacy_backup.vouchers_20261023 AS SELECT * FROM vouchers;

ALTER TABLE vouchers RENAME TO voucher_details;
ALTER SEQUENCE vouchers_id_seq RENAME TO voucher_details_id_seq;
ALTER INDEX vouchers_pkey RENAME TO voucher_details_pkey;
ALTER INDEX uq_vouchers_code RENAME TO uq_voucher_details_code;
ALTER INDEX idx_vouchers_customer RENAME TO idx_voucher_details_customer;
ALTER INDEX idx_vouchers_order RENAME TO idx_voucher_details_order;
ALTER INDEX idx_vouchers_incident RENAME TO idx_voucher_details_incident;
ALTER TABLE voucher_details RENAME CONSTRAINT vouchers_customer_id_fkey TO voucher_details_customer_id_fkey;
ALTER TABLE voucher_details RENAME CONSTRAINT vouchers_order_id_fkey TO voucher_details_order_id_fkey;
ALTER TABLE voucher_details RENAME CONSTRAINT vouchers_incident_id_fkey TO voucher_details_incident_id_fkey;
ALTER TABLE voucher_details RENAME CONSTRAINT vouchers_status_check TO voucher_details_status_check;
ALTER TABLE voucher_details RENAME CONSTRAINT vouchers_check TO voucher_details_check;

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

INSERT INTO vouchers (name, discount_value, min_order_value, quantity, valid_days)
SELECT CASE
           WHEN g.compensation THEN 'Đền bù sự cố ' || (g.discount_value / 1000)::int || 'K'
           WHEN g.shared THEN 'Chào mừng ' || (g.discount_value / 1000)::int || 'K'
           ELSE 'Tri ân khách hàng ' || (g.discount_value / 1000)::int || 'K'
       END,
       g.discount_value, g.min_order_value, GREATEST(100, g.cnt), g.days
FROM (SELECT discount_value, min_order_value, incident_id IS NOT NULL AS compensation, bool_and(customer_id IS NULL) AS shared,
             count(*) AS cnt, max(round(extract(epoch FROM expires_at - issued_at) / 86400))::int AS days
      FROM voucher_details
      GROUP BY discount_value, min_order_value, incident_id IS NOT NULL) g
ORDER BY g.compensation, g.discount_value;

ALTER TABLE voucher_details ADD COLUMN voucher_id BIGINT REFERENCES vouchers (id);
UPDATE voucher_details d SET voucher_id = v.id
FROM vouchers v
WHERE v.discount_value = d.discount_value AND v.min_order_value = d.min_order_value
  AND (v.name LIKE 'Đền bù%') = (d.incident_id IS NOT NULL);
ALTER TABLE voucher_details ALTER COLUMN voucher_id SET NOT NULL;
ALTER TABLE voucher_details DROP COLUMN discount_value, DROP COLUMN min_order_value;
CREATE INDEX idx_voucher_details_voucher ON voucher_details (voucher_id);

COMMIT;
