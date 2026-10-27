-- Notifications Table (for Admin & Vendor alerts)
CREATE TABLE IF NOT EXISTS notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    recipient_id UUID NOT NULL, -- Could be admin ID or specific user/vendor ID
    type VARCHAR(50) NOT NULL, -- PAYMENT_UPDATE, NEW_ORDER, STATUS_CHANGE
    title VARCHAR(200) NOT NULL,
    message TEXT NOT NULL,
    related_order_id UUID,
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_notifications_recipient ON notifications(recipient_id, is_read);