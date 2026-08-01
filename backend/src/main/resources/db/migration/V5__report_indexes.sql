CREATE INDEX idx_expenses_group_status_category_date
    ON expenses(group_id, status, category_id, expense_date);
