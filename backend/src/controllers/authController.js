const jwt = require('jsonwebtoken');
const User = require('../models/User');

// Mock OTP store (In production, use Twilio or Firebase Phone Auth).
// phoneNumber -> { code, expiresAt }
const otpStore = new Map();

// In-memory mock user store used as a fallback when no Postgres instance is
// reachable (e.g. local development without DATABASE_URL). Keeps the auth
// flow fully testable end-to-end before the database is provisioned.
const mockUsers = new Map(); // phoneNumber -> user object

const OTP_TTL_MS = 5 * 60 * 1000;
const JWT_EXPIRES_IN = '7d';
const JWT_SECRET = process.env.JWT_SECRET || 'dev-secret-change-me';

if (!process.env.JWT_SECRET) {
  console.warn('[auth] JWT_SECRET is not set - using a development fallback secret.');
}

function normalizePhoneNumber(raw) {
  return typeof raw === 'string' ? raw.trim() : '';
}

function isValidPhoneNumber(phoneNumber) {
  return /^\+\d{10,15}$/.test(phoneNumber);
}

function toPublicUser(user) {
  if (!user) return null;
  return {
    id: user.id,
    phoneNumber: user.phone || user.phone_number,
    fullName: user.name || user.full_name || null,
    state: user.state || null,
    lga: user.lga || null,
    address: user.address || null,
    isVendor: user.role === 'vendor' || user.is_vendor === true,
  };
}

function mockIdFor(phoneNumber) {
  return `mock-${phoneNumber.replace(/[^+\d]/g, '')}`;
}

function upsertMockUser(phoneNumber) {
  let user = mockUsers.get(phoneNumber);
  if (!user) {
    user = {
      id: mockIdFor(phoneNumber),
      phone: phoneNumber,
      name: null,
      state: null,
      lga: null,
      address: null,
      role: 'user',
      is_verified: true,
    };
    mockUsers.set(phoneNumber, user);
  }
  return user;
}

function signToken(user) {
  return jwt.sign(
    {
      userId: user.id,
      phoneNumber: user.phone || user.phone_number,
      role: user.role || 'user',
    },
    JWT_SECRET,
    { expiresIn: JWT_EXPIRES_IN }
  );
}

/**
 * POST /api/auth/register
 * Accepts a phone number, generates a mock OTP and returns success.
 * In development the OTP is also echoed back (mockOtp) for testing.
 */
exports.register = async (req, res) => {
  const phoneNumber = normalizePhoneNumber(
    req.body.phoneNumber || req.body.phone_number || req.body.phone
  );

  if (!phoneNumber || !isValidPhoneNumber(phoneNumber)) {
    return res.status(400).json({
      error: 'Invalid phone number format. Expected E.164 e.g. +2348012345678',
    });
  }

  // Generate 6-digit OTP
  const otp = Math.floor(100000 + Math.random() * 900000).toString();

  otpStore.set(phoneNumber, {
    code: otp,
    expiresAt: Date.now() + OTP_TTL_MS,
  });

  console.log(`[MOCK SMS] Sending OTP ${otp} to ${phoneNumber}`);

  // In a real app the OTP would be dispatched via Twilio/Firebase here.
  const includeMockOtp = process.env.NODE_ENV !== 'production';
  const payload = {
    success: true,
    message: 'OTP sent successfully',
  };
  if (includeMockOtp) {
    payload.mockOtp = otp; // REMOVE THIS IN PRODUCTION
  }
  return res.json(payload);
};

/**
 * POST /api/auth/verify-otp
 * Mock-verifies the OTP and returns a JWT token for the user.
 */
exports.verifyOtp = async (req, res) => {
  const phoneNumber = normalizePhoneNumber(
    req.body.phoneNumber || req.body.phone_number || req.body.phone
  );
  const otp = typeof req.body.otp === 'string' ? req.body.otp.trim() : String(req.body.otp || '');

  if (!phoneNumber || !otp) {
    return res.status(400).json({ error: 'Phone number and OTP are required' });
  }

  const stored = otpStore.get(phoneNumber);
  if (!stored) {
    return res
      .status(400)
      .json({ error: 'No OTP found for this number. Please request a new one.' });
  }

  if (Date.now() > stored.expiresAt) {
    otpStore.delete(phoneNumber);
    return res.status(400).json({ error: 'OTP expired. Please request a new one.' });
  }

  if (stored.code !== otp) {
    return res.status(400).json({ error: 'Invalid OTP' });
  }

  // Clear the OTP after successful verification
  otpStore.delete(phoneNumber);

  let user;
  let isNewUser = false;
  try {
    const existing = await User.findByPhone(phoneNumber);
    if (existing) {
      user = existing.is_verified ? existing : await User.setVerified(existing.id);
    } else {
      isNewUser = true;
      user = await User.create(phoneNumber);
      user = await User.setVerified(user.id);
    }
  } catch (dbError) {
    // Database unreachable - fall back to the mock store so the flow keeps
    // working in development. Log loudly so this never reaches production.
    console.warn('[auth] Database unavailable, using mock user store:', dbError.message);
    user = upsertMockUser(phoneNumber);
  }

  const token = signToken(user);

  return res.json({
    success: true,
    token,
    isNewUser,
    user: toPublicUser(user),
  });
};

/**
 * POST /api/auth/profile
 * Accepts Name, State, LGA and Address. The user id comes from the JWT that
 * the authentication middleware has already verified and attached to req.user.
 */
exports.updateProfile = async (req, res) => {
  const userId = req.user && req.user.userId;
  if (!userId) {
    return res.status(401).json({ error: 'Unauthorized: user id missing from token' });
  }

  const name = typeof req.body.name === 'string' ? req.body.name.trim() : '';
  const state = typeof req.body.state === 'string' ? req.body.state.trim() : '';
  const lga = typeof req.body.lga === 'string' ? req.body.lga.trim() : '';
  const address = typeof req.body.address === 'string' ? req.body.address.trim() : '';

  if (!name || !state || !lga || !address) {
    return res.status(400).json({
      error: 'Name, State, LGA and Address are all required',
    });
  }

  try {
    const updated = await User.updateProfile(userId, { name, state, lga, address });
    if (!updated) {
      return res.status(404).json({ error: 'User not found' });
    }
    return res.json({
      success: true,
      message: 'Profile updated successfully',
      user: toPublicUser(updated),
    });
  } catch (dbError) {
    // Database unreachable - mirror the update into the mock store so the
    // development flow keeps working end-to-end.
    console.warn('[auth] Database unavailable, using mock user store:', dbError.message);
    const mockUser = Array.from(mockUsers.values()).find((u) => u.id === userId);
    if (!mockUser) {
      return res.status(404).json({ error: 'User not found' });
    }
    mockUser.name = name;
    mockUser.state = state;
    mockUser.lga = lga;
    mockUser.address = address;
    return res.json({
      success: true,
      message: 'Profile updated successfully (mock)',
      user: toPublicUser(mockUser),
    });
  }
};

// Kept for backwards compatibility with tests/clients that referenced the old name.
exports.sendOtp = exports.register;
