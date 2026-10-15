-- Upgrade an existing shared PostgreSQL database for app-crm.
-- Uses plain SQL only; no PL/pgSQL DO blocks are required.
-- Safe to rerun. Does not alter Core-owned order data.
BEGIN;

CREATE TABLE IF NOT EXISTS customer_groups (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    description     TEXT,
    group_type      VARCHAR(20) NOT NULL DEFAULT 'MANUAL',
    criteria_rule   JSONB,
    recalculated_at TIMESTAMP,
    created_by      VARCHAR(50) NOT NULL DEFAULT 'system',
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_by      VARCHAR(50),
    updated_at      TIMESTAMP,
    del_flag        BOOLEAN NOT NULL DEFAULT FALSE,
    del_user        VARCHAR(50)
);

ALTER TABLE customer_groups ADD COLUMN IF NOT EXISTS group_type VARCHAR(20) NOT NULL DEFAULT 'MANUAL';
ALTER TABLE customer_groups ADD COLUMN IF NOT EXISTS criteria_rule JSONB;
ALTER TABLE customer_groups ADD COLUMN IF NOT EXISTS recalculated_at TIMESTAMP;

-- Support the previously used criteria_type column, then remove it after migration.
ALTER TABLE customer_groups ADD COLUMN IF NOT EXISTS criteria_type VARCHAR(30);
UPDATE customer_groups
SET group_type = CASE WHEN criteria_type = 'MANUAL' THEN 'MANUAL' ELSE 'AUTOMATIC' END
WHERE group_type = 'MANUAL' AND criteria_type IS NOT NULL;
ALTER TABLE customer_groups DROP COLUMN IF EXISTS criteria_type;

ALTER TABLE customer_groups DROP CONSTRAINT IF EXISTS ck_customer_groups_group_type;
ALTER TABLE customer_groups ADD CONSTRAINT ck_customer_groups_group_type
    CHECK (group_type IN ('AUTOMATIC', 'MANUAL'));

CREATE UNIQUE INDEX IF NOT EXISTS uq_customer_groups_name
    ON customer_groups (lower(name)) WHERE NOT del_flag;

CREATE TABLE IF NOT EXISTS customer_group_members (
    id          BIGSERIAL PRIMARY KEY,
    group_id    BIGINT NOT NULL REFERENCES customer_groups (id),
    customer_id BIGINT NOT NULL REFERENCES customers (id),
    is_manual   BOOLEAN NOT NULL DEFAULT FALSE,
    is_excluded BOOLEAN NOT NULL DEFAULT FALSE,
    added_at    TIMESTAMP NOT NULL DEFAULT now(),
    created_by  VARCHAR(50) NOT NULL DEFAULT 'system',
    created_at  TIMESTAMP NOT NULL DEFAULT now(),
    updated_by  VARCHAR(50),
    updated_at  TIMESTAMP,
    del_flag    BOOLEAN NOT NULL DEFAULT FALSE,
    del_user    VARCHAR(50)
);
ALTER TABLE customer_group_members ADD COLUMN IF NOT EXISTS is_excluded BOOLEAN NOT NULL DEFAULT FALSE;
CREATE UNIQUE INDEX IF NOT EXISTS uq_customer_group_members
    ON customer_group_members (group_id, customer_id) WHERE NOT del_flag;
CREATE INDEX IF NOT EXISTS idx_cgm_customer ON customer_group_members (customer_id);

ALTER TABLE interactions ADD COLUMN IF NOT EXISTS staff_id BIGINT REFERENCES staff (id);
ALTER TABLE feedbacks ADD COLUMN IF NOT EXISTS staff_id BIGINT REFERENCES staff (id);
ALTER TABLE customers ADD COLUMN IF NOT EXISTS date_of_birth DATE;
ALTER TABLE customers ADD COLUMN IF NOT EXISTS gender VARCHAR(20);
ALTER TABLE customers ADD COLUMN IF NOT EXISTS description TEXT;
ALTER TABLE feedbacks ADD COLUMN IF NOT EXISTS rating SMALLINT;
ALTER TABLE feedbacks ADD COLUMN IF NOT EXISTS issue_type VARCHAR(20);
ALTER TABLE interactions ADD COLUMN IF NOT EXISTS occurred_at TIMESTAMP;
UPDATE interactions SET occurred_at = created_at WHERE occurred_at IS NULL;
ALTER TABLE interactions ALTER COLUMN occurred_at SET DEFAULT now();
ALTER TABLE interactions ALTER COLUMN occurred_at SET NOT NULL;
ALTER TABLE interactions ADD COLUMN IF NOT EXISTS outcome TEXT;
ALTER TABLE interactions ADD COLUMN IF NOT EXISTS visible_to_customer BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE feedbacks DROP CONSTRAINT IF EXISTS ck_feedbacks_rating;
ALTER TABLE feedbacks ADD CONSTRAINT ck_feedbacks_rating CHECK (rating BETWEEN 1 AND 5);
ALTER TABLE feedbacks DROP CONSTRAINT IF EXISTS ck_feedbacks_issue_type;
ALTER TABLE feedbacks ADD CONSTRAINT ck_feedbacks_issue_type
    CHECK (issue_type IN ('PRODUCT', 'SERVICE', 'DELIVERY', 'PAYMENT', 'OTHER'));

CREATE TABLE IF NOT EXISTS feedback_topics (
    feedback_id BIGINT NOT NULL REFERENCES feedbacks (id),
    topic       VARCHAR(100) NOT NULL,
    PRIMARY KEY (feedback_id, topic)
);
INSERT INTO feedback_topics (feedback_id, topic)
SELECT id, trim(topic) FROM feedbacks WHERE topic IS NOT NULL AND trim(topic) <> ''
ON CONFLICT DO NOTHING;

CREATE TABLE IF NOT EXISTS feedback_responses (
    id          BIGSERIAL PRIMARY KEY,
    feedback_id BIGINT NOT NULL REFERENCES feedbacks (id),
    staff_id    BIGINT REFERENCES staff (id),
    message     TEXT NOT NULL,
    sent_at     TIMESTAMP NOT NULL DEFAULT now(),
    read_at     TIMESTAMP,
    created_by  VARCHAR(50) NOT NULL DEFAULT 'system',
    created_at  TIMESTAMP NOT NULL DEFAULT now(),
    updated_by  VARCHAR(50),
    updated_at  TIMESTAMP,
    del_flag    BOOLEAN NOT NULL DEFAULT FALSE,
    del_user    VARCHAR(50)
);
ALTER TABLE feedback_responses ADD COLUMN IF NOT EXISTS read_at TIMESTAMP;
CREATE INDEX IF NOT EXISTS idx_feedback_responses_feedback ON feedback_responses (feedback_id, sent_at);

COMMIT;
