-- Bốn nhóm phân loại khách cố định. Ngưỡng nằm trong criteria_rule và được ADMIN cấu hình.
ALTER TABLE customer_groups
    ADD COLUMN IF NOT EXISTS classification_code VARCHAR(30);

CREATE UNIQUE INDEX IF NOT EXISTS uq_customer_groups_classification_code
    ON customer_groups (classification_code)
    WHERE classification_code IS NOT NULL AND del_flag = FALSE;

-- Gắn mã vào các nhóm hiện hữu theo tên; không tạo bản ghi trùng nếu đã có.
UPDATE customer_groups SET classification_code = 'REGULAR', group_type = 'AUTOMATIC'
WHERE lower(trim(name)) IN ('khách hàng thường', 'khach hang thuong') AND del_flag = FALSE
  AND classification_code IS NULL;
UPDATE customer_groups SET classification_code = 'POTENTIAL', group_type = 'AUTOMATIC'
WHERE lower(trim(name)) IN ('khách hàng tiềm năng', 'khach hang tiem nang') AND del_flag = FALSE
  AND classification_code IS NULL;
UPDATE customer_groups SET classification_code = 'VIP', group_type = 'AUTOMATIC'
WHERE lower(trim(name)) IN ('khách hàng vip', 'khach hang vip') AND del_flag = FALSE
  AND classification_code IS NULL;
UPDATE customer_groups SET classification_code = 'CHURN_RISK', group_type = 'AUTOMATIC'
WHERE lower(trim(name)) IN ('khách hàng nguy cơ rời bỏ', 'khach hang nguy co roi bo') AND del_flag = FALSE
  AND classification_code IS NULL;

INSERT INTO customer_groups (name, description, group_type, classification_code, criteria_rule, del_flag, created_by, created_at)
SELECT v.name, v.description, 'AUTOMATIC', v.code, v.rule::jsonb, FALSE, 'system', current_timestamp
FROM (VALUES
 ('Khách hàng thường','Chi tiêu dưới ngưỡng Tiềm năng','REGULAR','{"minSpend":0}'),
 ('Khách hàng tiềm năng','Chi tiêu từ ngưỡng Tiềm năng đến dưới ngưỡng VIP','POTENTIAL','{}'),
 ('Khách hàng VIP','Chi tiêu từ ngưỡng VIP trở lên','VIP','{}'),
 ('Khách hàng nguy cơ rời bỏ','Đã mua trước đây nhưng quá số ngày cấu hình chưa quay lại','CHURN_RISK','{"inactiveDays":60}')
) AS v(name,description,code,rule)
WHERE NOT EXISTS (SELECT 1 FROM customer_groups g WHERE g.classification_code = v.code AND g.del_flag = FALSE);

UPDATE customer_groups SET criteria_rule='{"minSpend":0,"maxSpend":30000000}'::jsonb
WHERE classification_code='REGULAR' AND del_flag=FALSE AND (criteria_rule IS NULL OR criteria_rule->>'maxSpend' IS NULL);
UPDATE customer_groups SET criteria_rule='{"minSpend":30000000,"maxSpend":100000000}'::jsonb
WHERE classification_code='POTENTIAL' AND del_flag=FALSE AND (criteria_rule IS NULL OR criteria_rule->>'minSpend' IS NULL OR criteria_rule->>'maxSpend' IS NULL);
UPDATE customer_groups SET criteria_rule='{"minSpend":100000000}'::jsonb
WHERE classification_code='VIP' AND del_flag=FALSE AND (criteria_rule IS NULL OR criteria_rule->>'minSpend' IS NULL);
UPDATE customer_groups SET criteria_rule='{"inactiveDays":60}'::jsonb
WHERE classification_code='CHURN_RISK' AND del_flag=FALSE AND (criteria_rule IS NULL OR criteria_rule->>'inactiveDays' IS NULL);
