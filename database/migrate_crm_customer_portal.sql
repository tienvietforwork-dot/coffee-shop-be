-- Enables staff to explicitly expose selected interaction records to customers.
-- Existing rows remain private because the new flag defaults to FALSE.
ALTER TABLE interactions
    ADD COLUMN IF NOT EXISTS visible_to_customer BOOLEAN NOT NULL DEFAULT FALSE;

-- Stores the first time a customer marks a staff response as read.
ALTER TABLE feedback_responses
    ADD COLUMN IF NOT EXISTS read_at TIMESTAMP;
