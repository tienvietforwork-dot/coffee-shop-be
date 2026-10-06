-- =====================================================================
-- Chuyển các bảng của schema cũ (app-core v1) ra khỏi public sang
-- schema legacy_backup để nhường chỗ cho schema.sql.
-- Giữ lại bảng users ở public (đăng nhập JWT của app-core vẫn dùng).
-- Dữ liệu không bị xóa; muốn bỏ hẳn: DROP SCHEMA legacy_backup CASCADE;
-- =====================================================================

BEGIN;

CREATE SCHEMA IF NOT EXISTS legacy_backup;

ALTER TABLE IF EXISTS public.material_transactions SET SCHEMA legacy_backup;
ALTER TABLE IF EXISTS public.product_materials     SET SCHEMA legacy_backup;
ALTER TABLE IF EXISTS public.shipments             SET SCHEMA legacy_backup;
ALTER TABLE IF EXISTS public.payments              SET SCHEMA legacy_backup;
ALTER TABLE IF EXISTS public.order_items           SET SCHEMA legacy_backup;
ALTER TABLE IF EXISTS public.orders                SET SCHEMA legacy_backup;
ALTER TABLE IF EXISTS public.products              SET SCHEMA legacy_backup;
ALTER TABLE IF EXISTS public.categories            SET SCHEMA legacy_backup;
ALTER TABLE IF EXISTS public.materials             SET SCHEMA legacy_backup;
ALTER TABLE IF EXISTS public.daily_revenue         SET SCHEMA legacy_backup;
ALTER TABLE IF EXISTS public.flyway_schema_history SET SCHEMA legacy_backup;

COMMIT;
