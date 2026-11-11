-- Enables customers to reply within an existing complaint/suggestion conversation.
-- Existing feedback response rows are staff replies by default.
ALTER TABLE feedback_responses
    ADD COLUMN IF NOT EXISTS sender_type VARCHAR(20) NOT NULL DEFAULT 'STAFF';


