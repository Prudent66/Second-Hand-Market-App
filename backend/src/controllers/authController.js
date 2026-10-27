const db = require('../config/db');

// Mock OTP store (In production, use Twilio or Firebase Phone Auth)
const otpStore = new Map(); // phoneNumber -> { code, expiresAt }

exports.sendOtp = async (req, res) => {
    const { phoneNumber } = req.body;

    if (!phoneNumber || !/^+\d{10,15}$/.test(phoneNumber)) {
        return res.status(400).json({ error: 'Invalid phone number format' });
    }

    // Generate 6-digit OTP
    const otp = Math.floor(100000 + Math.random() * 900000).toString();
    
    // Store OTP with expiration (5 minutes)
    otpStore.set(phoneNumber, {
        code: otp,
        expiresAt: Date.now() + 5 * 60 * 1000
    });

    console.log(`[MOCK SMS] Sending OTP ${otp} to ${phoneNumber}`);
    
    // In a real app, you would send this via Twilio/Firebase here.
    // For now, we return it in the response for testing purposes ONLY.
    res.json({ 
        message: 'OTP sent successfully', 
        mockOtp: otp // REMOVE THIS IN PRODUCTION
    });
};

exports.verifyOtp = async (req, res) => {
    const { phoneNumber, otp } = req.body;

    if (!phoneNumber || !otp) {
        return res.status(400).json({ error: 'Phone number and OTP are required' });
    }

    const storedData = otpStore.get(phoneNumber);

    if (!storedData) {
        return res.status(400).json({ error: 'No OTP found for this number. Please request a new one.' });
    }

    if (Date.now() > storedData.expiresAt) {
        otpStore.delete(phoneNumber);
        return res.status(400).json({ error: 'OTP expired. Please request a new one.' });
    }

    if (storedData.code !== otp) {
        return res.status(400).json({ error: 'Invalid OTP' });
    }

    // Clear OTP after successful verification
    otpStore.delete(phoneNumber);

    // Check if user exists, otherwise create new user
    let userResult = await db.query('SELECT * FROM users WHERE phone_number = $1', [phoneNumber]);
    let user;

    if (userResult.rows.length === 0) {
        // Create new user
        const insertResult = await db.query(
            'INSERT INTO users (phone_number, is_vendor) VALUES ($1, FALSE) RETURNING *',
            [phoneNumber]
        );
        user = insertResult.rows[0];
    } else {
        user = userResult.rows[0];
    }

    // Generate JWT Token
    const jwt = require('jsonwebtoken');
    const token = jwt.sign(
        { userId: user.id, phoneNumber: user.phone_number },
        process.env.JWT_SECRET,
        { expiresIn: '7d' }
    );

    res.json({
        token,
        user: {
            id: user.id,
            phoneNumber: user.phone_number,
            fullName: user.full_name,
            isVendor: user.is_vendor
        }
    });
};