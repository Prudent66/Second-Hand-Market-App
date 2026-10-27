const express = require('express');
const router = express.Router();
const vendorController = require('../controllers/vendorController');
const authenticateToken = require('../middleware/authMiddleware');

router.post('/register', authenticateToken, vendorController.registerAsVendor);
router.get('/me', authenticateToken, vendorController.getVendorDetails);

module.exports = router;