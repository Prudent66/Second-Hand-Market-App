const db = require('../config/db');

exports.registerAsVendor = async (req, res) => {
  const { bankAccountNumber, bankName, accountHolderName, businessName } = req.body;
  const userId = req.user.userId;

  try {
    // Check if already a vendor
    const existing = await db.query('SELECT * FROM vendors WHERE user_id = $1', [userId]);
    if (existing.rows.length > 0) return res.status(400).json({ error: 'Already registered as vendor' });

    // Update user flag
    await db.query('UPDATE users SET is_vendor = TRUE WHERE id = $1', [userId]);

    // Insert vendor details
    const result = await db.query(
      'INSERT INTO vendors (user_id, bank_account_number, bank_name, account_holder_name, business_name) VALUES ($1, $2, $3, $4, $5) RETURNING *',
      [userId, bankAccountNumber, bankName, accountHolderName, businessName]
    );
    
    res.status(201).json(result.rows[0]);
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
};

exports.getVendorDetails = async (req, res) => {
  const userId = req.user.userId;
  try {
    const result = await db.query('SELECT * FROM vendors WHERE user_id = $1', [userId]);
    if (result.rows.length === 0) return res.status(404).json({ error: 'Vendor profile not found' });
    res.json(result.rows[0]);
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
};