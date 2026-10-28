const db = require('../config/db');

class User {
  static async createTable() {
    const query = `
      CREATE TABLE IF NOT EXISTS users (
        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
        phone VARCHAR(20) UNIQUE NOT NULL,
        name VARCHAR(100),
        state VARCHAR(50),
        lga VARCHAR(50),
        address TEXT,
        role VARCHAR(20) DEFAULT 'user' CHECK (role IN ('user', 'vendor', 'admin')),
        is_verified BOOLEAN DEFAULT FALSE,
        created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
      );
      CREATE INDEX IF NOT EXISTS idx_users_phone ON users(phone);
      CREATE INDEX IF NOT EXISTS idx_users_role ON users(role);
    `;
    await db.query(query);
  }

  static async findByPhone(phone) {
    const result = await db.query('SELECT * FROM users WHERE phone = $1', [phone]);
    return result.rows[0] || null;
  }

  static async findById(id) {
    const result = await db.query('SELECT * FROM users WHERE id = $1', [id]);
    return result.rows[0] || null;
  }

  static async create(phone, role = 'user') {
    const result = await db.query(
      'INSERT INTO users (phone, role) VALUES ($1, $2) RETURNING *',
      [phone, role]
    );
    return result.rows[0];
  }

  static async updateProfile(id, { name, state, lga, address }) {
    const result = await db.query(
      `UPDATE users 
       SET name = COALESCE($1, name),
           state = COALESCE($2, state),
           lga = COALESCE($3, lga),
           address = COALESCE($4, address),
           updated_at = CURRENT_TIMESTAMP
       WHERE id = $5
       RETURNING *`,
      [name, state, lga, address, id]
    );
    return result.rows[0];
  }

  static async setVerified(id) {
    const result = await db.query(
      'UPDATE users SET is_verified = TRUE, updated_at = CURRENT_TIMESTAMP WHERE id = $1 RETURNING *',
      [id]
    );
    return result.rows[0];
  }

  static async setRole(id, role) {
    const result = await db.query(
      'UPDATE users SET role = $1, updated_at = CURRENT_TIMESTAMP WHERE id = $2 RETURNING *',
      [role, id]
    );
    return result.rows[0];
  }

  static async getAllVendors() {
    const result = await db.query(`
      SELECT u.*, vd.bank_account_number, vd.bank_name, vd.account_holder_name
      FROM users u
      LEFT JOIN vendor_details vd ON u.id = vd.user_id
      WHERE u.role = 'vendor'
    `);
    return result.rows;
  }
}

module.exports = User;