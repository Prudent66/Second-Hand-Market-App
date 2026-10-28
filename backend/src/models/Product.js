const db = require('../config/db');

class Product {
  static async createTable() {
    const query = `
      CREATE TABLE IF NOT EXISTS products (
        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
        vendor_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
        title VARCHAR(200) NOT NULL,
        description TEXT,
        price DECIMAL(12, 2) NOT NULL CHECK (price >= 0),
        image_url TEXT,
        category VARCHAR(50),
        condition VARCHAR(20) CHECK (condition IN ('new', 'like_new', 'good', 'fair', 'poor')),
        status VARCHAR(20) DEFAULT 'available' CHECK (status IN ('available', 'reserved', 'sold', 'hidden')),
        created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
      );
      CREATE INDEX IF NOT EXISTS idx_products_vendor ON products(vendor_id);
      CREATE INDEX IF NOT EXISTS idx_products_status ON products(status);
      CREATE INDEX IF NOT EXISTS idx_products_category ON products(category);
      CREATE INDEX IF NOT EXISTS idx_products_created ON products(created_at DESC);
    `;
    await db.query(query);
  }

  static async create(vendorId, { title, description, price, imageUrl, category, condition }) {
    const result = await db.query(
      `INSERT INTO products (vendor_id, title, description, price, image_url, category, condition)
       VALUES ($1, $2, $3, $4, $5, $6, $7)
       RETURNING *`,
      [vendorId, title, description, price, imageUrl, category, condition || 'good']
    );
    return result.rows[0];
  }

  static async findById(id) {
    const result = await db.query(
      `SELECT p.*, u.name as vendor_name, u.phone as vendor_phone, vd.bank_account_number, vd.bank_name, vd.account_holder_name
       FROM products p
       JOIN users u ON p.vendor_id = u.id
       LEFT JOIN vendor_details vd ON u.id = vd.user_id
       WHERE p.id = $1`,
      [id]
    );
    return result.rows[0] || null;
  }

  static async findByVendor(vendorId, { status, limit = 20, offset = 0 } = {}) {
    let query = 'SELECT * FROM products WHERE vendor_id = $1';
    const params = [vendorId];
    
    if (status) {
      query += ' AND status = $2';
      params.push(status);
    }
    
    query += ' ORDER BY created_at DESC LIMIT $' + (params.length + 1) + ' OFFSET $' + (params.length + 2);
    params.push(limit, offset);
    
    const result = await db.query(query, params);
    return result.rows;
  }

  static async findAll({ category, status = 'available', search, limit = 20, offset = 0 } = {}) {
    let query = `
      SELECT p.*, u.name as vendor_name, u.state, u.lga
      FROM products p
      JOIN users u ON p.vendor_id = u.id
      WHERE p.status = $1
    `;
    const params = [status];
    let paramIndex = 2;

    if (category) {
      query += ` AND p.category = $${paramIndex}`;
      params.push(category);
      paramIndex++;
    }

    if (search) {
      query += ` AND (p.title ILIKE $${paramIndex} OR p.description ILIKE $${paramIndex})`;
      params.push(`%${search}%`);
      paramIndex++;
    }

    query += ` ORDER BY p.created_at DESC LIMIT $${paramIndex} OFFSET $${paramIndex + 1}`;
    params.push(limit, offset);

    const result = await db.query(query, params);
    return result.rows;
  }

  static async update(id, vendorId, updates) {
    const allowedFields = ['title', 'description', 'price', 'image_url', 'category', 'condition', 'status'];
    const setClause = [];
    const values = [];
    let paramIndex = 1;

    for (const [key, value] of Object.entries(updates)) {
      if (allowedFields.includes(key) && value !== undefined) {
        setClause.push(`${key} = $${paramIndex}`);
        values.push(value);
        paramIndex++;
      }
    }

    if (setClause.length === 0) return null;

    setClause.push(`updated_at = CURRENT_TIMESTAMP`);
    values.push(id, vendorId);

    const query = `
      UPDATE products
      SET ${setClause.join(', ')}
      WHERE id = $${paramIndex} AND vendor_id = $${paramIndex + 1}
      RETURNING *
    `;

    const result = await db.query(query, values);
    return result.rows[0] || null;
  }

  static async delete(id, vendorId) {
    const result = await db.query(
      'DELETE FROM products WHERE id = $1 AND vendor_id = $2 RETURNING *',
      [id, vendorId]
    );
    return result.rows[0] || null;
  }

  static async getCategories() {
    const result = await db.query(
      'SELECT DISTINCT category FROM products WHERE category IS NOT NULL ORDER BY category'
    );
    return result.rows.map(r => r.category);
  }
}

module.exports = Product;