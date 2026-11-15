-- Kiểm soát người giao hàng: phân công đơn giao cho nhân viên, người giao xem / cập nhật đơn của mình,
-- tuyến giao trên bản đồ. Người giao là nhân viên (vẫn 3 role), quyền mới "my-deliveries".
-- Chạy tay trên Neon (an toàn khi chạy lại).
BEGIN;

ALTER TABLE shipments ADD COLUMN IF NOT EXISTS shipper_id   BIGINT REFERENCES staff (id);
ALTER TABLE shipments ADD COLUMN IF NOT EXISTS assigned_at  TIMESTAMP;
ALTER TABLE shipments ADD COLUMN IF NOT EXISTS picked_up_at TIMESTAMP;   -- lúc người giao nhận đồ, bắt đầu đi
CREATE INDEX IF NOT EXISTS idx_shipments_shipper ON shipments (shipper_id) WHERE NOT del_flag;

INSERT INTO permissions (code, name, path, module, description, sort_order)
SELECT 'my-deliveries', 'Đơn giao của tôi', '/admin/my-deliveries', 'core', 'Người giao: đơn được phân công, tuyến đường, cập nhật trạng thái', 32
WHERE NOT EXISTS (SELECT 1 FROM permissions p WHERE p.code = 'my-deliveries' AND NOT p.del_flag);

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON p.code = 'my-deliveries' AND NOT p.del_flag
WHERE r.code IN ('ADMIN', 'STAFF')
  AND NOT EXISTS (SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id AND NOT rp.del_flag);

COMMIT;
