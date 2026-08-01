CREATE TABLE personal_expenses (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    category_id BIGINT NULL,
    title VARCHAR(180) NOT NULL,
    note VARCHAR(1000) NULL,
    amount BIGINT NOT NULL,
    expense_date DATE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_personal_expenses_user_date (user_id, expense_date),
    CONSTRAINT chk_personal_expenses_amount CHECK (amount > 0),
    CONSTRAINT fk_personal_expenses_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_personal_expenses_category FOREIGN KEY (category_id) REFERENCES categories(id)
);

CREATE TABLE monthly_budgets (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    budget_month DATE NOT NULL,
    amount BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_monthly_budgets_user_month UNIQUE (user_id, budget_month),
    CONSTRAINT chk_monthly_budgets_amount CHECK (amount > 0),
    CONSTRAINT fk_monthly_budgets_user FOREIGN KEY (user_id) REFERENCES users(id)
);
