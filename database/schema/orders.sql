-- Orders Table
CREATE TABLE IF NOT EXISTS orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    buyer_id UUID REFERENCES users(id) ON DELETE SET NULL,
    product_id UUID REFERENCES products(id) ON DELETE RESTRICT,
    vendor_id UUID REFERENCES vendors(id) ON DELETE RESTRICT,
    quantity INT DEFAULT 1,
    total_price DECIMAL(10, 2) NOT NULL,
    payment_method VARCHAR(20) NOT NULL, -- BANK_TRANSFER, PAY_ON_DELIVERY
    payment_status VARCHAR(30) DEFAULT 'AWAITING_PAYMENT', 
      -- States: AWAITING_PAYMENT, PAYMENT_RECEIVED, DELIVERED, CANCELLED
    delivery_address TEXT NOT NULL,
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_orders_buyer ON orders(buyer_id);
CREATE INDEX idx_orders_vendor ON orders(vendor_id);
CREATE INDEX idx_orders_payment_status ON orders(payment_status);