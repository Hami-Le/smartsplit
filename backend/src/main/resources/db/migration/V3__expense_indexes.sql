CREATE INDEX idx_expenses_group_status_date
    ON expenses(group_id, status, expense_date);

CREATE INDEX idx_expense_payers_user
    ON expense_payers(user_id, expense_id);

CREATE INDEX idx_expense_shares_user
    ON expense_shares(user_id, expense_id);
