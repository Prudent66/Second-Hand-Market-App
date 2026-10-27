-- Products/Goods Table
CREATE TABLE IF NOT EXISTS products (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    vendor_id UUID REFERENCES vendors(id) ON DELETE CASCADE,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    price DECIMAL(10, 2) NOT NULL,
    category VARCHAR(50) NOT NULL, -- Electronics, Furniture, Fashion, etc.
    condition VARCHAR(20) NOT NULL, -- New, Like New, Used, Fair
    images JSONB NOT NULL, -- Array of image URLs
    location_state VARCHAR(50),
    location_lga VARCHAR(50),
    status VARCHAR(20) DEFAULT 'AVAILABLE', -- AVAILABLE, SOLD, RESERVED
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_products_category ON products(category);
CREATE INDEX idx_products_status ON products(status);