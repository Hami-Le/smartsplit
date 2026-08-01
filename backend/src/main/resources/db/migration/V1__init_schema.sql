CREATE TABLE users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    full_name VARCHAR(120) NOT NULL,
    email VARCHAR(190) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    avatar_url VARCHAR(500) NULL,
    phone VARCHAR(30) NULL,
    provider VARCHAR(30) NOT NULL DEFAULT 'LOCAL',
    role VARCHAR(30) NOT NULL DEFAULT 'USER',
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_users_email UNIQUE (email)
);

CREATE TABLE expense_groups (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(150) NOT NULL,
    description VARCHAR(500) NULL,
    avatar_url VARCHAR(500) NULL,
    default_currency VARCHAR(3) NOT NULL DEFAULT 'VND',
    created_by BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_groups_created_by FOREIGN KEY (created_by) REFERENCES users(id)
);

CREATE TABLE group_members (
    id BIGINT NOT NULL AUTO_INCREMENT,
    group_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    joined_at DATETIME(6) NOT NULL,
    left_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_group_members_group_user UNIQUE (group_id, user_id),
    CONSTRAINT fk_group_members_group FOREIGN KEY (group_id) REFERENCES expense_groups(id),
    CONSTRAINT fk_group_members_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE group_invitations (
    id BIGINT NOT NULL AUTO_INCREMENT,
    group_id BIGINT NOT NULL,
    email VARCHAR(190) NOT NULL,
    token_hash VARCHAR(255) NOT NULL,
    invited_by BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    expires_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_group_invitations_token UNIQUE (token_hash),
    CONSTRAINT fk_group_invitations_group FOREIGN KEY (group_id) REFERENCES expense_groups(id),
    CONSTRAINT fk_group_invitations_user FOREIGN KEY (invited_by) REFERENCES users(id)
);

CREATE TABLE categories (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(80) NOT NULL,
    icon VARCHAR(80) NULL,
    is_system BOOLEAN NOT NULL DEFAULT TRUE,
    created_by BIGINT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_categories_created_by FOREIGN KEY (created_by) REFERENCES users(id)
);

CREATE TABLE expenses (
    id BIGINT NOT NULL AUTO_INCREMENT,
    group_id BIGINT NOT NULL,
    category_id BIGINT NULL,
    title VARCHAR(180) NOT NULL,
    description VARCHAR(1000) NULL,
    total_amount BIGINT NOT NULL,
    expense_date DATE NOT NULL,
    created_by BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_expenses_group_date (group_id, expense_date),
    CONSTRAINT chk_expenses_amount CHECK (total_amount > 0),
    CONSTRAINT fk_expenses_group FOREIGN KEY (group_id) REFERENCES expense_groups(id),
    CONSTRAINT fk_expenses_category FOREIGN KEY (category_id) REFERENCES categories(id),
    CONSTRAINT fk_expenses_created_by FOREIGN KEY (created_by) REFERENCES users(id)
);

CREATE TABLE expense_payers (
    id BIGINT NOT NULL AUTO_INCREMENT,
    expense_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    paid_amount BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_expense_payers_expense_user UNIQUE (expense_id, user_id),
    CONSTRAINT chk_expense_payers_amount CHECK (paid_amount > 0),
    CONSTRAINT fk_expense_payers_expense FOREIGN KEY (expense_id) REFERENCES expenses(id),
    CONSTRAINT fk_expense_payers_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE expense_shares (
    id BIGINT NOT NULL AUTO_INCREMENT,
    expense_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    share_amount BIGINT NOT NULL,
    share_percentage DECIMAL(7,4) NULL,
    split_type VARCHAR(20) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_expense_shares_expense_user UNIQUE (expense_id, user_id),
    CONSTRAINT chk_expense_shares_amount CHECK (share_amount >= 0),
    CONSTRAINT fk_expense_shares_expense FOREIGN KEY (expense_id) REFERENCES expenses(id),
    CONSTRAINT fk_expense_shares_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE settlements (
    id BIGINT NOT NULL AUTO_INCREMENT,
    group_id BIGINT NOT NULL,
    payer_id BIGINT NOT NULL,
    receiver_id BIGINT NOT NULL,
    amount BIGINT NOT NULL,
    note VARCHAR(500) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    settled_at DATETIME(6) NULL,
    created_by BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_settlements_group_created (group_id, created_at),
    CONSTRAINT chk_settlements_amount CHECK (amount > 0),
    CONSTRAINT chk_settlements_users CHECK (payer_id <> receiver_id),
    CONSTRAINT fk_settlements_group FOREIGN KEY (group_id) REFERENCES expense_groups(id),
    CONSTRAINT fk_settlements_payer FOREIGN KEY (payer_id) REFERENCES users(id),
    CONSTRAINT fk_settlements_receiver FOREIGN KEY (receiver_id) REFERENCES users(id),
    CONSTRAINT fk_settlements_created_by FOREIGN KEY (created_by) REFERENCES users(id)
);

CREATE TABLE attachments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    expense_id BIGINT NOT NULL,
    file_url VARCHAR(700) NOT NULL,
    file_type VARCHAR(100) NOT NULL,
    ocr_status VARCHAR(20) NULL,
    ocr_result JSON NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_attachments_expense FOREIGN KEY (expense_id) REFERENCES expenses(id)
);

CREATE TABLE notifications (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    type VARCHAR(40) NOT NULL,
    title VARCHAR(180) NOT NULL,
    content VARCHAR(1000) NOT NULL,
    reference_id BIGINT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_notifications_user_read (user_id, is_read, created_at),
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE refresh_tokens (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    token_hash VARCHAR(255) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    revoked_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_refresh_tokens_hash UNIQUE (token_hash),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE audit_logs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NULL,
    action VARCHAR(80) NOT NULL,
    entity_type VARCHAR(80) NOT NULL,
    entity_id BIGINT NULL,
    old_value JSON NULL,
    new_value JSON NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_audit_entity (entity_type, entity_id),
    CONSTRAINT fk_audit_logs_user FOREIGN KEY (user_id) REFERENCES users(id)
);

INSERT INTO categories(name, icon, is_system) VALUES
('Ăn uống', 'utensils', TRUE),
('Di chuyển', 'car', TRUE),
('Khách sạn', 'hotel', TRUE),
('Mua sắm', 'shopping-bag', TRUE),
('Giải trí', 'gamepad', TRUE),
('Khác', 'circle-ellipsis', TRUE);
