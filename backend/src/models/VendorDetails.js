const db = require('../config/db');

class VendorDetails {
  static async createTable() {
    const query = `
      CREATE TABLE IF NOT EXISTS vendor_details (
        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
        user_id UUID UNIQUE NOT NULL REFERENCES users(id) ON DELETE CASCADE,
        bank_account_number VARCHAR(20) NOT NULL,
        bank_name VARCHAR(100) NOT NULL,
        account_holder_name VARCHAR(100) NOT NULL,
        business_name VARCHAR(100),
        business_address TEXT,
        created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
      );
      CREATE INDEX IF NOT EXISTS idx_vendor_details_user ON vendor_details(user_id);
    `;
    await db.query(query);
  }

  static async create(userId, { bankAccountNumber, bankName, accountHolderName, businessName, businessAddress }) {
    const result = await db.query(
      `INSERT INTO vendor_details (user_id, bank_account_number, bank_name, account_holder_name, business_name, business_address)
       VALUES ($1, $2, $3, $4, $5, $6)
       RETURNING *`,
      [userId, bankAccountNumber, bankName, accountHolderName, businessName, businessAddress]
    );
    return result.rows[0];
  }

  static async findByUserId(userId) {
    const result = await db.query('SELECT * FROM vendor_details WHERE user_id = $1', [userId]);
    return result.rows[0] || null;
  }

  static async update(userId, { bankAccountNumber, bankName, accountHolderName, businessName, businessAddress }) {
    const result = await db.query(
      `UPDATE vendor_details
       SET bank_account_number = COALESCE($1, bank_account_number),
           bank_name = COALESCE($2, bank_name),
           account_holder_name = COALESCE($3, account_holder_name),
           business_name = COALESCE($4, business_name),
           business_address = COALESCE($5, business_address),
           updated_at = CURRENT_TIMESTAMP
       WHERE user_id = $6
       RETURNING *`,
      [bankAccountNumber, bankName, accountHolderName, businessName, businessAddress, userId]
    );
    return result.rows[0];
  }

  static async upsert(userId, data) {
    const existing = await this.findByUserId(userId);
    if (existing) {
      return this.update(userId, data);
    }
    return this.create(userId, data);
  }
}

module.exports = VendorDetails;