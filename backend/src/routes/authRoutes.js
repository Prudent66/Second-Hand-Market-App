const express = require('express');
const router = express.Router();
const authController = require('../controllers/authController');
const authenticateToken = require('../middleware/authMiddleware');

// POST /api/auth/register - phone number in, mock OTP out
router.post('/register', authController.register);

// POST /api/auth/verify-otp - phone + OTP in, JWT out
router.post('/verify-otp', authController.verifyOtp);

// POST /api/auth/profile - JWT protected profile update (Name, State, LGA, Address)
router.post('/profile', authenticateToken, authController.updateProfile);

// Legacy alias for older clients that used the previous endpoint name.
router.post('/send-otp', authController.register);

module.exports = router;
