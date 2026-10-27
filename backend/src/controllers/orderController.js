const db = require('../config/db');

exports.createOrder = async (req, res) => {
  const { productId, paymentMethod, deliveryAddress, notes } = req.body;
  const buyerId = req.user.userId;

  try {
    // Get product details to ensure availability and get vendor info
    const productRes = await db.query('SELECT * FROM products WHERE id = $1 AND status = 'AVAILABLE'', [productId]);
    if (productRes.rows.length === 0) return res.status(404).json({ error: 'Product not available' });
    
    const product = productRes.rows[0];
    
    // Create Order
    const orderResult = await db.query(
      'INSERT INTO orders (buyer_id, product_id, vendor_id, total_price, payment_method, payment_status, delivery_address, notes) VALUES ($1, $2, $3, $4, $5, $6, $7, $8) RETURNING *',
      [buyerId, productId, product.vendor_id, product.price, paymentMethod, 'AWAITING_PAYMENT', deliveryAddress, notes]
    );
    
    // Update Product Status to RESERVED
    await db.query('UPDATE products SET status = 'RESERVED' WHERE id = $1', [productId]);
    
    res.status(201).json(orderResult.rows[0]);
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
};

exports.updatePaymentStatus = async (req, res) => {
  const { orderId } = req.params;
  const { status } = req.body; // PAYMENT_RECEIVED, DELIVERED, CANCELLED
  const vendorId = req.user.vendorId;

  try {
    // Verify ownership
    const orderCheck = await db.query('SELECT * FROM orders WHERE id = $1 AND vendor_id = $2', [orderId, vendorId]);
    if (orderCheck.rows.length === 0) return res.status(403).json({ error: 'Unauthorized' });
    
    const order = orderCheck.rows[0];
    
    // Update Status
    const result = await db.query(
      'UPDATE orders SET payment_status = $1, updated_at = CURRENT_TIMESTAMP WHERE id = $2 RETURNING *',
      [status, orderId]
    );
    
    // Notify Admin (Insert into notifications table)
    await db.query(
      'INSERT INTO notifications (recipient_id, type, title, message, related_order_id) VALUES ($1, $2, $3, $4, $5)',
      ['ADMIN_ID_PLACEHOLDER', 'PAYMENT_UPDATE', 'Order Status Updated', 'New status: ' + status, orderId]
    );
    
    // If Delivered, mark product as Sold
    if (status === 'DELIVERED') {
       await db.query('UPDATE products SET status = 'SOLD' WHERE id = $1', [order.product_id]);
    } else if (status === 'CANCELLED') {
       await db.query('UPDATE products SET status = 'AVAILABLE' WHERE id = $1', [order.product_id]);
    }

    res.json(result.rows[0]);
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
};

exports.getOrdersForVendor = async (req, res) => {
  const vendorId = req.user.vendorId;
  try {
    const result = await db.query(
      'SELECT o.*, u.full_name as buyer_name, p.title as product_title FROM orders o JOIN users u ON o.buyer_id = u.id JOIN products p ON o.product_id = p.id WHERE o.vendor_id = $1 ORDER BY o.created_at DESC',
      [vendorId]
    );
    res.json(result.rows);
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
};