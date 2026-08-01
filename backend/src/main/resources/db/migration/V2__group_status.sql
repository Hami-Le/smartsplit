ALTER TABLE expense_groups
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' AFTER default_currency;

CREATE INDEX idx_expense_groups_status ON expense_groups(status);
