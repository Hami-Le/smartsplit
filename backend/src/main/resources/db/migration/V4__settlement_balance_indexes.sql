CREATE INDEX idx_settlements_group_status_settled
    ON settlements(group_id, status, settled_at);
