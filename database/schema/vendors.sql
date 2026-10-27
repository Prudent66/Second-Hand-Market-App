-- Vendors Table (extends User capabilities)
CREATE TABLE IF NOT EXISTS vendors (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    bank_account_number VARCHAR(30) NOT NULL,
    bank_name VARCHAR(50) NOT NULL,
    account_holder_name VARCHAR(100) NOT NULL,
    business_name VARCHAR(100),
    subscription_status VARCHAR(20) DEFAULT 'FREE', -- Placeholder for future fees
    rating DECIMAL(2,1) DEFAULT 0.0,
    total_sales INT DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_vendors_user ON vendors(user_id);