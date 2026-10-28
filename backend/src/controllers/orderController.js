const db = require('../config/db');

const PAYMENT_STATUSES = ['pending', 'received', 'delivered', 'cancelled'];

exports.createOrder = async (req, res) => {
  const { productId, paymentMethod, deliveryAddress, notes } = req.body;
  const buyerId = req.user && req.user.userId;

  try {
    // Get product details to ensure availability and get vendor info
    const productRes = await db.query(
      'SELECT * FROM products WHERE id = $1 AND status = $2',
      [productId, 'available']
    );
    if (productRes.rows.length === 0) {return res.status(404).json({ error: 'Product not available' });}

    const product = productRes.rows[0];

    // Create Order
    const orderResult = await db.query(
      `INSERT INTO orders (buyer_id, product_id, vendor_id, total_price, payment_method, payment_status, delivery_address, notes)
       VALUES ($1, $2, $3, $4, $5, $6, $7, $8)
       RETURNING *`,
      [buyerId, productId, product.vendor_id, product.price, paymentMethod, 'pending', deliveryAddress, notes]
    );

    // Update Product Status to RESERVED
    await db.query('UPDATE products SET status = $1 WHERE id = $2', ['reserved', productId]);

    res.status(201).json(orderResult.rows[0]);
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
};

exports.updatePaymentStatus = async (req, res) => {
  const { id } = req.params;
  const requestedStatus = typeof req.body.status === 'string' ? req.body.status.toLowerCase() : '';
  const vendorId = (req.user && req.user.userId) || (req.user && req.user.vendorId);

  if (!PAYMENT_STATUSES.includes(requestedStatus)) {
    return res.status(400).json({ error: `Invalid status. Allowed: ${PAYMENT_STATUSES.join(', ')}` });
  }

  try {
    // Verify ownership
    const orderCheck = await db.query('SELECT * FROM orders WHERE id = $1 AND vendor_id = $2', [id, vendorId]);
    if (orderCheck.rows.length === 0) {return res.status(403).json({ error: 'Unauthorized' });}

    const order = orderCheck.rows[0];

    // Update Status
    const result = await db.query(
      'UPDATE orders SET payment_status = $1, updated_at = CURRENT_TIMESTAMP WHERE id = $2 RETURNING *',
      [requestedStatus, id]
    );

    // If Delivered, mark product as Sold. If Cancelled, make it available again.
    if (requestedStatus === 'delivered') {
      await db.query('UPDATE products SET status = $1 WHERE id = $2', ['sold', order.product_id]);
    } else if (requestedStatus === 'cancelled') {
      await db.query('UPDATE products SET status = $1 WHERE id = $2', ['available', order.product_id]);
    }

    res.json(result.rows[0]);
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
};

exports.getOrdersForVendor = async (req, res) => {
  const vendorId = (req.user && req.user.userId) || (req.user && req.user.vendorId);
  try {
    const result = await db.query(
      `SELECT o.*, u.name AS buyer_name, p.title AS product_title
       FROM orders o
       JOIN users u ON o.buyer_id = u.id
       JOIN products p ON o.product_id = p.id
       WHERE o.vendor_id = $1
       ORDER BY o.created_at DESC`,
      [vendorId]
    );
    res.json(result.rows);
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
};
