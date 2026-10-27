const express = require('express');
const router = express.Router();
const orderController = require('../controllers/orderController');
const authenticateToken = require('../middleware/authMiddleware');

router.post('/', authenticateToken, orderController.createOrder);
router.patch('/:id/status', authenticateToken, orderController.updatePaymentStatus);
router.get('/vendor', authenticateToken, orderController.getOrdersForVendor);

module.exports = router;