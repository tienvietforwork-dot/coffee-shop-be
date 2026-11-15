-- Thiết lập hóa đơn điện tử (trang Hóa đơn điện tử → Thiết lập): người bán, loại hóa đơn, ký hiệu.
-- Mỗi hóa đơn chụp lại người bán lúc lập. Chạy được nhiều lần.
BEGIN;

CREATE TABLE IF NOT EXISTS invoice_settings (
    id              BIGINT PRIMARY KEY CHECK (id = 1),     -- 1 dòng duy nhất
    seller_name     VARCHAR(255)  NOT NULL,
    seller_tax_code VARCHAR(14)   NOT NULL,
    seller_address  VARCHAR(255),                          -- NULL = địa chỉ quán (delivery_settings.shop_address)
    vat_rate        NUMERIC(5,2)  CHECK (vat_rate BETWEEN 0 AND 10), -- NULL = hóa đơn bán hàng (mẫu 2)
    series_suffix   VARCHAR(2)    NOT NULL DEFAULT 'CH',   -- 2 chữ cuối ký hiệu, người bán tự đặt
    tax_office_code VARCHAR(5),                            -- XXXXX trong mã CQT M1-YY-XXXXX-NNNNNNNNNNN
    lookup_url      VARCHAR(255),
    created_by VARCHAR(50)  NOT NULL DEFAULT 'system',
    created_at TIMESTAMP    NOT NULL DEFAULT now(),
    updated_by VARCHAR(50),
    updated_at TIMESTAMP,
    del_flag   BOOLEAN      NOT NULL DEFAULT FALSE,
    del_user   VARCHAR(50)
);

INSERT INTO invoice_settings (id, seller_name, seller_tax_code, series_suffix, lookup_url)
VALUES (1, 'HỘ KINH DOANH COFFEEHOLIC', '0312345678', 'CH', 'https://www.meinvoice.vn/tra-cuu')
ON CONFLICT (id) DO NOTHING;

-- người bán in trên hóa đơn = lúc lập (hóa đơn cũ: NULL → hiện theo thiết lập hiện tại)
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS seller_name     VARCHAR(255);
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS seller_tax_code VARCHAR(14);
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS seller_address  VARCHAR(255);

COMMIT;
