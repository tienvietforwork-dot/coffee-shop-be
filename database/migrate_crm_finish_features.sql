-- CRM: hoàn thiện nhắc chăm sóc, dấu đã đọc, duyệt chuyển cấp và báo cáo xu hướng phản hồi.
-- Chạy một lần trên database PostgreSQL dùng chung trước khi khởi động phiên bản app-crm mới.

ALTER TABLE interactions
    ADD COLUMN IF NOT EXISTS customer_read_at TIMESTAMP;

ALTER TABLE feedbacks
    ADD COLUMN IF NOT EXISTS approval_status VARCHAR(20) NOT NULL DEFAULT 'NOT_REQUIRED',
    ADD COLUMN IF NOT EXISTS approval_note TEXT,
    ADD COLUMN IF NOT EXISTS approved_by VARCHAR(50),
    ADD COLUMN IF NOT EXISTS approved_at TIMESTAMP;

-- Những ca chuyển cấp cũ đã được xử lý theo luồng cũ, không đưa chúng vào trạng thái chờ duyệt.
UPDATE feedbacks
SET approval_status = 'APPROVED'
WHERE approval_status = 'NOT_REQUIRED'
  AND (escalated = TRUE OR status = 'ESCALATED');

CREATE TABLE IF NOT EXISTS interaction_reminder_notifications (
    id BIGSERIAL PRIMARY KEY,
    interaction_id BIGINT NOT NULL REFERENCES interactions(id),
    staff_id BIGINT,
    remind_at TIMESTAMP NOT NULL,
    read_at TIMESTAMP,
    created_by VARCHAR(50) NOT NULL DEFAULT 'system',
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_by VARCHAR(50),
    updated_at TIMESTAMP,
    del_flag BOOLEAN NOT NULL DEFAULT FALSE,
    del_user VARCHAR(50)
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_interaction_reminder_delivery
    ON interaction_reminder_notifications(interaction_id, remind_at);
CREATE INDEX IF NOT EXISTS idx_interaction_reminder_unread
    ON interaction_reminder_notifications(read_at, remind_at)
    WHERE del_flag = FALSE;
