const db = require('../config/db');

exports.getProducts = async (req, res) => {
  const { category, search } = req.query;
  
  try {
    let query = 'SELECT p.*, v.business_name, u.full_name as seller_name FROM products p JOIN vendors v ON p.vendor_id = v.id JOIN users u ON v.user_id = u.id WHERE p.status = 'AVAILABLE'';
    const params = [];
    
    if (category) {
      query += ' AND p.category = $' + (params.push(category));
    }
    if (search) {
      query += ' AND (p.title ILIKE $' + (params.push('%'+search+'%')) + ' OR p.description ILIKE $' + (params.push('%'+search+'%'))) ;
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
      'SELECT p.*, v.bank_account_number, v.bank_name FROM products p JOIN vendors v ON p.vendor_id = v.id WHERE p.id = $1',
      [id]
    );
    if (result.rows.length === 0) return res.status(404).json({ error: 'Product not found' });
    res.json(result.rows[0]);
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
};

exports.createProduct = async (req, res) => {
  const { title, description, price, category, condition, images, locationState, locationLga } = req.body;
  const vendorId = req.user.vendorId; // Assuming middleware attaches vendorId
  
  try {
    const result = await db.query(
      'INSERT INTO products (vendor_id, title, description, price, category, condition, images, location_state, location_lga) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9) RETURNING *',
      [vendorId, title, description, price, category, condition, JSON.stringify(images), locationState, locationLga]
    );
    res.status(201).json(result.rows[0]);
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
};