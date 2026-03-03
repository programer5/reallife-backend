ALTER TABLE notifications
    ADD COLUMN ref2_id UUID NULL;

CREATE INDEX idx_notification_user_type_ref2
    ON notifications (user_id, type, ref2_id);