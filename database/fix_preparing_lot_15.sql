-- Một lần: lô bán thành phẩm #15 (Cốt cà phê phin) còn kẹt ở PREPARING sau khi bỏ bước "đang chế biến".
-- Chuyển sang AVAILABLE với sản lượng dự kiến, hạn dùng = bây giờ + hạn dùng chuẩn, giá vốn từ nguyên liệu đã trừ.
-- Chạy lại lần 2 không làm gì (mọi lệnh chỉ chạy khi lô còn PREPARING).
BEGIN;

UPDATE materials m SET stock_quantity = m.stock_quantity + b.import_quantity, updated_by = 'admin', updated_at = now()
FROM material_batches b
WHERE b.id = 15 AND b.status = 'PREPARING' AND m.id = b.material_id;

INSERT INTO material_transactions (batch_id, type, quantity, note, created_by, created_at, del_flag)
SELECT b.id, 'PRODUCE', b.import_quantity, 'Hoàn tất lô chế biến (chuyển sang chế biến 1 bước)', 'admin', now(), false
FROM material_batches b WHERE b.id = 15 AND b.status = 'PREPARING';

UPDATE material_batches b
SET status             = 'AVAILABLE',
    remaining_quantity = b.import_quantity,
    ready_at           = now(),
    expires_at         = now() + make_interval(mins => m.shelf_life_minutes),
    expiry_date        = (now() + make_interval(mins => m.shelf_life_minutes))::date,
    unit_cost          = round((SELECT -sum(t.quantity * ib.unit_cost)
                                FROM material_transactions t JOIN material_batches ib ON ib.id = t.batch_id
                                WHERE t.produced_batch_id = b.id) / b.import_quantity, 2),
    updated_by         = 'admin',
    updated_at         = now()
FROM materials m
WHERE m.id = b.material_id AND b.id = 15 AND b.status = 'PREPARING';

COMMIT;
