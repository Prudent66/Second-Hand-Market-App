const db = require('../config/db');

const AVAILABLE_STATUSES = ['available', 'reserved', 'sold'];

exports.getProducts = async (req, res) => {
  const { category, search, status } = req.query;

  try {
    let query = `
      SELECT p.*, u.name AS seller_name, u.phone AS seller_phone
      FROM products p
      JOIN users u ON p.vendor_id = u.id
      WHERE p.status = $1
    `;
    const params = [status && AVAILABLE_STATUSES.includes(status) ? status : 'available'];
    let paramIndex = 2;

    if (category) {
      query += ` AND p.category = $${paramIndex}`;
      params.push(category);
      paramIndex += 1;
    }
    if (search) {
      query += ` AND (p.title ILIKE $${paramIndex} OR p.description ILIKE $${paramIndex})`;
      params.push(`%${search}%`);
      paramIndex += 1;
    }

    query += ' ORDER BY p.created_at DESC';

    const result = await db.query(query, params);
    res.json(result.rows);
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
};

exports.getProductById = async (req, res) => {
  const { id } = req.params;
  try {
    const result = await db.query(
      `SELECT p.*, u.name AS vendor_name, u.phone AS vendor_phone,
              vd.bank_account_number, vd.bank_name, vd.account_holder_name
       FROM products p
       JOIN users u ON p.vendor_id = u.id
       LEFT JOIN vendor_details vd ON u.id = vd.user_id
       WHERE p.id = $1`,
      [id]
    );
    if (result.rows.length === 0) return res.status(404).json({ error: 'Product not found' });
    res.json(result.rows[0]);
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
};

exports.createProduct = async (req, res) => {
  const { title, description, price, category, condition, imageUrl } = req.body;
  const vendorId = (req.user && req.user.userId) || (req.user && req.user.vendorId);

  try {
    const result = await db.query(
      `INSERT INTO products (vendor_id, title, description, price, image_url, category, condition)
       VALUES ($1, $2, $3, $4, $5, $6, $7)
       RETURNING *`,
      [vendorId, title, description, price, imageUrl || null, category, condition || 'good']
    );
    res.status(201).json(result.rows[0]);
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
};
