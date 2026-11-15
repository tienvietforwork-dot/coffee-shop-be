-- CRM enhancement schema. Review and apply manually after deploying the matching app-crm version.
-- This file is intentionally not executed by the implementation task.

CREATE TABLE IF NOT EXISTS customer_profile_history (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL REFERENCES customers(id),
    actor VARCHAR(100) NOT NULL,
    before_values JSONB NOT NULL,
    after_values JSONB NOT NULL,
    changed_at TIMESTAMP NOT NULL DEFAULT current_timestamp
);
CREATE INDEX IF NOT EXISTS idx_customer_profile_history_customer ON customer_profile_history(customer_id, changed_at DESC);

CREATE TABLE IF NOT EXISTS customer_surveys (
    id BIGSERIAL PRIMARY KEY,
    feedback_id BIGINT NOT NULL UNIQUE REFERENCES feedbacks(id),
    customer_id BIGINT NOT NULL REFERENCES customers(id),
    order_id BIGINT REFERENCES orders(id),
    source_feedback_id BIGINT REFERENCES feedbacks(id),
    question VARCHAR(500) NOT NULL,
    answer TEXT NOT NULL,
    recommendation_score SMALLINT CHECK (recommendation_score BETWEEN 0 AND 10),
    created_at TIMESTAMP NOT NULL DEFAULT current_timestamp
);
CREATE INDEX IF NOT EXISTS idx_customer_surveys_created ON customer_surveys(created_at);
ALTER TABLE customer_surveys ADD COLUMN IF NOT EXISTS satisfaction_score SMALLINT
    CHECK (satisfaction_score BETWEEN 1 AND 5);

CREATE TABLE IF NOT EXISTS crm_manager_notifications (
    id BIGSERIAL PRIMARY KEY,
    feedback_id BIGINT NOT NULL REFERENCES feedbacks(id),
    notification_type VARCHAR(40) NOT NULL,
    message TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT current_timestamp,
    read_at TIMESTAMP,
    del_flag BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uq_crm_manager_notification UNIQUE(feedback_id, notification_type)
);
CREATE INDEX IF NOT EXISTS idx_crm_manager_notifications_unread ON crm_manager_notifications(read_at, created_at DESC) WHERE del_flag=FALSE;
