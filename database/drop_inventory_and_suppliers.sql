-- Bỏ nghiệp vụ kho và nhà cung cấp: không còn nhập / xuất kho, lô, tồn, kiểm kê, báo cáo xuất nhập tồn,
-- giá vốn, bán thành phẩm (định lượng chuẩn, quy cách đóng gói), nhà cung cấp, phiếu đặt hàng, phiếu nhập,
-- trả hàng nhập, công nợ / phiếu chi NCC.
-- Nguyên liệu chỉ còn tên + đơn vị; định lượng nằm trong công thức (recipe_materials).
-- Chạy SAU khi deploy app-core mới (bản cũ còn đọc các bảng / cột này). Không hoàn tác được: sao lưu trước nếu cần.
BEGIN;

-- món bị hệ thống tự chuyển "hết món" vì thiếu kho: mở bán lại (giờ trạng thái món chỉ đổi bằng tay)
UPDATE coffees SET status = 'AVAILABLE', updated_by = 'system', updated_at = now()
WHERE auto_sold_out AND status = 'SOLD_OUT';
-- cột giữ lại vì app-promotions / app-stats còn map
UPDATE coffees SET auto_sold_out = FALSE WHERE auto_sold_out;

DROP TABLE IF EXISTS
    stocktake_items, stocktakes,
    inventory_snapshots, coffee_cost_snapshots,
    material_transactions,
    supplier_return_items, supplier_returns, supplier_payments,
    goods_receipt_items, goods_receipts, material_batches,
    purchase_order_items, purchase_orders,
    supplier_materials, suppliers,
    material_components, material_packages;

-- nguyên liệu đã "ngừng dùng" (INACTIVE, cách xóa cũ): xóa mềm luôn nếu không còn công thức nào dùng,
-- trước khi bỏ cột status (nếu không chúng hiện lại trong danh sách)
UPDATE materials m SET del_flag = TRUE, del_user = 'system', updated_by = 'system', updated_at = now()
WHERE m.status = 'INACTIVE' AND NOT m.del_flag
  AND NOT EXISTS (SELECT 1 FROM recipe_materials rm WHERE rm.material_id = m.id AND NOT rm.del_flag);

ALTER TABLE materials
    DROP COLUMN IF EXISTS stock_quantity,
    DROP COLUMN IF EXISTS min_stock,
    DROP COLUMN IF EXISTS expiry_warning_days,
    DROP COLUMN IF EXISTS status,
    DROP COLUMN IF EXISTS kind,
    DROP COLUMN IF EXISTS yield_quantity,
    DROP COLUMN IF EXISTS prep_minutes,
    DROP COLUMN IF EXISTS shelf_life_minutes,
    DROP COLUMN IF EXISTS instructions;

-- quyền màn hình đã bỏ
DELETE FROM role_permissions WHERE permission_id IN (SELECT id FROM permissions WHERE code IN (
    'stock', 'stock-history', 'suppliers', 'purchase-orders', 'supplier-returns', 'supplier-debts',
    'stocktakes', 'inventory-report', 'coffee-costs'));
DELETE FROM permissions WHERE code IN (
    'stock', 'stock-history', 'suppliers', 'purchase-orders', 'supplier-returns', 'supplier-debts',
    'stocktakes', 'inventory-report', 'coffee-costs');
UPDATE permissions SET name = 'Nguyên liệu' WHERE code = 'materials';
UPDATE permissions SET name = 'Sửa nguyên liệu', description = 'Thêm/sửa/xóa nguyên liệu (tên, đơn vị)'
WHERE code = 'materials.edit';

-- lượt chạy của background service đã bỏ (cập nhật trạng thái món theo kho)
DELETE FROM background_service_logs WHERE service = 'CoffeeStatusBackgroundService';

COMMIT;
