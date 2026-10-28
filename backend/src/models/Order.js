const db = require('../config/db');

class Order {
  static async createTable() {
    const query = `
      CREATE TABLE IF NOT EXISTS orders (
        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
        product_id UUID NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
        buyer_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
        vendor_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
        payment_method VARCHAR(20) NOT NULL CHECK (payment_method IN ('transfer', 'pay_on_delivery')),
        payment_status VARCHAR(20) DEFAULT 'pending' CHECK (payment_status IN ('pending', 'received', 'delivered', 'cancelled')),
        delivery_address TEXT,
        buyer_notes TEXT,
        vendor_notes TEXT,
        created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
      );
      CREATE INDEX IF NOT EXISTS idx_orders_buyer ON orders(buyer_id);
      CREATE INDEX IF NOT EXISTS idx_orders_vendor ON orders(vendor_id);
      CREATE INDEX IF NOT EXISTS idx_orders_product ON orders(product_id);
      CREATE INDEX IF NOT EXISTS idx_orders_payment_status ON orders(payment_status);
      CREATE INDEX IF NOT EXISTS idx_orders_created ON orders(created_at DESC);
    `;
    await db.query(query);
  }

  static async create(buyerId, productId, { paymentMethod, deliveryAddress, buyerNotes }) {
    const client = await db.pool.connect();
    try {
      await client.query('BEGIN');

      // Get product and lock it
      const productResult = await client.query(
        'SELECT * FROM products WHERE id = $1 AND status = $2 FOR UPDATE',
        [productId, 'available']
      );

      if (productResult.rows.length === 0) {
        throw new Error('Product not available');
      }

      const product = productResult.rows[0];

      // Create order
      const orderResult = await client.query(
        `INSERT INTO orders (product_id, buyer_id, vendor_id, payment_method, delivery_address, buyer_notes)
         VALUES ($1, $2, $3, $4, $5, $6)
         RETURNING *`,
        [productId, buyerId, product.vendor_id, paymentMethod, deliveryAddress, buyerNotes]
      );

      // Update product status to reserved
      await client.query(
        'UPDATE products SET status = $1, updated_at = CURRENT_TIMESTAMP WHERE id = $2',
        ['reserved', productId]
      );

      await client.query('COMMIT');
      return orderResult.rows[0];
    } catch (error) {
      await client.query('ROLLBACK');
      throw error;
    } finally {
      client.release();
    }
  }

  static async findById(id) {
    const result = await db.query(
      `SELECT o.*, 
              p.title as product_title, p.price as product_price, p.image_url as product_image,
              b.name as buyer_name, b.phone as buyer_phone, b.address as buyer_address,
              v.name as vendor_name, v.phone as vendor_phone,
              vd.bank_account_number, vd.bank_name, vd.account_holder_name
       FROM orders o
       JOIN products p ON o.product_id = p.id
       JOIN users b ON o.buyer_id = b.id
       JOIN users v ON o.vendor_id = v.id
       LEFT JOIN vendor_details vd ON v.id = vd.user_id
       WHERE o.id = $1`,
      [id]
    );
    return result.rows[0] || null;
  }

  static async findByBuyer(buyerId, { limit = 20, offset = 0 } = {}) {
    const result = await db.query(
      `SELECT o.*, p.title as product_title, p.price as product_price, p.image_url as product_image,
              v.name as vendor_name, v.phone as vendor_phone
       FROM orders o
       JOIN products p ON o.product_id = p.id
       JOIN users v ON o.vendor_id = v.id
       WHERE o.buyer_id = $1
       ORDER BY o.created_at DESC
       LIMIT $2 OFFSET $3`,
      [buyerId, limit, offset]
    );
    return result.rows;
  }

  static async findByVendor(vendorId, { paymentStatus, limit = 20, offset = 0 } = {}) {
    let query = `
      SELECT o.*, p.title as product_title, p.price as product_price, p.image_url as product_image,
              b.name as buyer_name, b.phone as buyer_phone, b.address as buyer_address
      FROM orders o
      JOIN products p ON o.product_id = p.id
      JOIN users b ON o.buyer_id = b.id
      WHERE o.vendor_id = $1
    `;
    const params = [vendorId];
    let paramIndex = 2;

    if (paymentStatus) {
      query += ` AND o.payment_status = $${paramIndex}`;
      params.push(paymentStatus);
      paramIndex++;
    }

    query += ` ORDER BY o.created_at DESC LIMIT $${paramIndex} OFFSET $${paramIndex + 1}`;
    params.push(limit, offset);

    const result = await db.query(query, params);
    return result.rows;
  }

  static async updatePaymentStatus(orderId, vendorId, paymentStatus, vendorNotes = null) {
    const client = await db.pool.connect();
    try {
      await client.query('BEGIN');

      // Verify order belongs to vendor and get current status
      const orderResult = await client.query(
        'SELECT * FROM orders WHERE id = $1 AND vendor_id = $2 FOR UPDATE',
        [orderId, vendorId]
      );

      if (orderResult.rows.length === 0) {
        throw new Error('Order not found or unauthorized');
      }

      const order = orderResult.rows[0];

      // Validate status transition
      const validTransitions = {
        'pending': ['received', 'cancelled'],
        'received': ['delivered', 'cancelled'],
        'delivered': [],
        'cancelled': []
      };

      if (!validTransitions[order.payment_status]?.includes(paymentStatus)) {
        throw new Error(`Invalid status transition from ${order.payment_status} to ${paymentStatus}`);
      }

      // Update order
      const updateResult = await client.query(
        `UPDATE orders
         SET payment_status = $1,
             vendor_notes = COALESCE($2, vendor_notes),
             updated_at = CURRENT_TIMESTAMP
         WHERE id = $3
         RETURNING *`,
        [paymentStatus, vendorNotes, orderId]
      );

      // Update product status based on order status
      if (paymentStatus === 'delivered') {
        await client.query(
          'UPDATE products SET status = $1, updated_at = CURRENT_TIMESTAMP WHERE id = $2',
          ['sold', order.product_id]
        );
      } else if (paymentStatus === 'cancelled') {
        await client.query(
          'UPDATE products SET status = $1, updated_at = CURRENT_TIMESTAMP WHERE id = $2',
          ['available', order.product_id]
        );
      }

      await client.query('COMMIT');
      return updateResult.rows[0];
    } catch (error) {
      await client.query('ROLLBACK');
      throw error;
    } finally {
      client.release();
    }
  }

  static async cancelByBuyer(orderId, buyerId) {
    return this.updatePaymentStatus(orderId, buyerId, 'cancelled');
  }
}

module.exports = Order;