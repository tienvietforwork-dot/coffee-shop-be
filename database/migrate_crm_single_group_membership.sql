-- Chạy một lần trên database PostgreSQL đang dùng chung sau khi triển khai app-crm.
-- Giữ một nhóm cho mỗi khách; các liên kết trùng được xóa mềm, không xóa hồ sơ khách.
BEGIN;

-- Không giữ liên kết còn hoạt động trong nhóm đã bị xóa mềm.
UPDATE customer_group_members gm
SET del_flag = TRUE, del_user = 'system', updated_at = CURRENT_TIMESTAMP
FROM customer_groups g
WHERE g.id = gm.group_id AND g.del_flag = TRUE
  AND gm.del_flag = FALSE AND gm.is_excluded = FALSE;

WITH group_sizes AS (
    SELECT group_id, COUNT(*) AS member_count
    FROM customer_group_members
    WHERE del_flag = FALSE AND is_excluded = FALSE
    GROUP BY group_id
), ranked_members AS (
    SELECT gm.id,
           ROW_NUMBER() OVER (
               PARTITION BY gm.customer_id
               ORDER BY gm.is_manual DESC,
                        CASE WHEN g.group_type = 'AUTOMATIC' THEN COALESCE(gs.member_count, 0) ELSE 0 END ASC,
                        gm.added_at DESC,
                        g.id ASC,
                        gm.id ASC
           ) AS membership_rank
    FROM customer_group_members gm
    JOIN customer_groups g ON g.id = gm.group_id AND g.del_flag = FALSE
    LEFT JOIN group_sizes gs ON gs.group_id = gm.group_id
    WHERE gm.del_flag = FALSE AND gm.is_excluded = FALSE
)
UPDATE customer_group_members gm
SET del_flag = TRUE, del_user = 'system', updated_at = CURRENT_TIMESTAMP
FROM ranked_members ranked
WHERE gm.id = ranked.id AND ranked.membership_rank > 1;

CREATE UNIQUE INDEX IF NOT EXISTS uq_customer_group_members_one_active_group
    ON customer_group_members (customer_id)
    WHERE del_flag = FALSE AND is_excluded = FALSE;

-- Priority đã bị loại khỏi giao diện và logic; bỏ khóa cũ nếu dữ liệu còn lưu.
UPDATE customer_groups
SET criteria_rule = criteria_rule - 'priority'
WHERE criteria_rule IS NOT NULL AND criteria_rule ? 'priority';

COMMIT;
