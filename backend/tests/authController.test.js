const request = require('supertest');
const express = require('express');
const jwt = require('jsonwebtoken');

process.env.JWT_SECRET = 'test-secret';

// Mock the database
jest.mock('../src/config/db', () => ({
  query: jest.fn()
}));

const db = require('../src/config/db');
const authRoutes = require('../src/routes/authRoutes');
const authenticateToken = require('../src/middleware/authMiddleware');

const app = express();
app.use(express.json());
app.use('/api/auth', authRoutes);

// Minimal app that only applies the middleware, used for direct middleware tests
const middlewareApp = express();
middlewareApp.use(express.json());
middlewareApp.post('/protected', authenticateToken, (req, res) => res.json({ user: req.user }));

const verifiedUser = {
  id: 'user-123',
  phone: '+2348012345678',
  name: 'Test User',
  state: 'Gombe',
  lga: 'Akko',
  address: '12 Sample Street',
  role: 'user',
  is_verified: true
};

describe('Auth Controller', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    process.env.JWT_SECRET = 'test-secret';
  });

  describe('POST /api/auth/register', () => {
    it('should generate a mock OTP and return success', async () => {
      const response = await request(app)
        .post('/api/auth/register')
        .send({ phoneNumber: '+2348012345678' })
        .expect(200);

      expect(response.body).toHaveProperty('success', true);
      expect(response.body).toHaveProperty('message', 'OTP sent successfully');
      expect(response.body).toHaveProperty('mockOtp');
      expect(response.body.mockOtp).toMatch(/^\d{6}$/);
    });

    it('should return 400 for invalid phone number', async () => {
      const response = await request(app)
        .post('/api/auth/register')
        .send({ phoneNumber: 'invalid' })
        .expect(400);

      expect(response.body).toHaveProperty('error');
    });

    it('should return 400 when phone number is missing', async () => {
      const response = await request(app)
        .post('/api/auth/register')
        .send({})
        .expect(400);

      expect(response.body).toHaveProperty('error');
    });
  });

  describe('POST /api/auth/verify-otp', () => {
    it('should verify OTP and return a JWT token for an existing user', async () => {
      // Request the OTP first
      const sendResponse = await request(app)
        .post('/api/auth/register')
        .send({ phoneNumber: '+2348012345678' });
      const mockOtp = sendResponse.body.mockOtp;

      // Existing (unverified) user found, then verification update returns
      db.query.mockResolvedValueOnce({
        rows: [{ ...verifiedUser, is_verified: false }]
      });
      db.query.mockResolvedValueOnce({ rows: [verifiedUser] });

      const response = await request(app)
        .post('/api/auth/verify-otp')
        .send({ phoneNumber: '+2348012345678', otp: mockOtp })
        .expect(200);

      expect(response.body).toHaveProperty('success', true);
      expect(response.body).toHaveProperty('token');
      expect(response.body.isNewUser).toBe(false);
      expect(response.body.user).toHaveProperty('id', 'user-123');
      expect(response.body.user).toHaveProperty('phoneNumber', '+2348012345678');

      const decoded = jwt.verify(response.body.token, 'test-secret');
      expect(decoded.userId).toBe('user-123');
      expect(decoded.phoneNumber).toBe('+2348012345678');
    });

    it('should create a new user when the phone number is unknown', async () => {
      const sendResponse = await request(app)
        .post('/api/auth/register')
        .send({ phoneNumber: '+2349012345678' });
      const mockOtp = sendResponse.body.mockOtp;

      db.query.mockResolvedValueOnce({ rows: [] }); // findByPhone -> empty
      db.query.mockResolvedValueOnce({
        rows: [{ id: 'new-user-456', phone: '+2349012345678', name: null, role: 'user', is_verified: false }]
      }); // create
      db.query.mockResolvedValueOnce({
        rows: [{ id: 'new-user-456', phone: '+2349012345678', name: null, role: 'user', is_verified: true }]
      }); // setVerified

      const response = await request(app)
        .post('/api/auth/verify-otp')
        .send({ phoneNumber: '+2349012345678', otp: mockOtp })
        .expect(200);

      expect(response.body.isNewUser).toBe(true);
      expect(response.body.user.id).toBe('new-user-456');
    });

    it('should fall back to a mock user when the database is unreachable', async () => {
      const sendResponse = await request(app)
        .post('/api/auth/register')
        .send({ phoneNumber: '+2347012345678' });
      const mockOtp = sendResponse.body.mockOtp;

      db.query.mockRejectedValue(new Error('ECONNREFUSED'));

      const response = await request(app)
        .post('/api/auth/verify-otp')
        .send({ phoneNumber: '+2347012345678', otp: mockOtp })
        .expect(200);

      expect(response.body).toHaveProperty('token');
      expect(response.body.user.id).toMatch(/^mock-/);
    });

    it('should return 400 for an invalid OTP', async () => {
      await request(app).post('/api/auth/register').send({ phoneNumber: '+2348012345678' });

      const response = await request(app)
        .post('/api/auth/verify-otp')
        .send({ phoneNumber: '+2348012345678', otp: '000000' })
        .expect(400);

      expect(response.body).toHaveProperty('error', 'Invalid OTP');
    });

    it('should return 400 when no OTP was requested for the number', async () => {
      const response = await request(app)
        .post('/api/auth/verify-otp')
        .send({ phoneNumber: '+2348012345678', otp: '123456' })
        .expect(400);

      expect(response.body).toHaveProperty('error');
    });
  });

  describe('POST /api/auth/profile', () => {
    const profilePayload = {
      name: 'Aliyu Musa',
      state: 'Gombe',
      lga: 'Akko',
      address: '12 Sample Street, Gombe'
    };

    it('should return 401 when no token is provided', async () => {
      const response = await request(app)
        .post('/api/auth/profile')
        .send(profilePayload)
        .expect(401);

      expect(response.body).toHaveProperty('error');
    });

    it('should return 403 for an invalid token', async () => {
      const response = await request(app)
        .post('/api/auth/profile')
        .set('Authorization', 'Bearer not-a-real-token')
        .send(profilePayload)
        .expect(403);

      expect(response.body).toHaveProperty('error');
    });

    it('should return 400 when required fields are missing', async () => {
      const token = jwt.sign({ userId: 'user-123' }, 'test-secret');

      const response = await request(app)
        .post('/api/auth/profile')
        .set('Authorization', `Bearer ${token}`)
        .send({ name: 'Aliyu Musa' })
        .expect(400);

      expect(response.body).toHaveProperty('error');
    });

    it('should update the profile for a valid token', async () => {
      const token = jwt.sign(
        { userId: 'user-123', phoneNumber: '+2348012345678', role: 'user' },
        'test-secret'
      );

      db.query.mockResolvedValueOnce({
        rows: [{ ...verifiedUser, name: 'Aliyu Musa', address: '12 Sample Street, Gombe' }]
      });

      const response = await request(app)
        .post('/api/auth/profile')
        .set('Authorization', `Bearer ${token}`)
        .send(profilePayload)
        .expect(200);

      expect(response.body).toHaveProperty('success', true);
      expect(response.body.user).toHaveProperty('fullName', 'Aliyu Musa');
      expect(db.query).toHaveBeenCalledTimes(1);
    });

    it('should fall back to the mock store when the database is unreachable', async () => {
      // Seed the mock store by verifying a number while the DB is down
      const sendResponse = await request(app)
        .post('/api/auth/register')
        .send({ phoneNumber: '+2347012345678' });
      db.query.mockRejectedValue(new Error('ECONNREFUSED'));

      const verifyResponse = await request(app)
        .post('/api/auth/verify-otp')
        .send({ phoneNumber: '+2347012345678', otp: sendResponse.body.mockOtp })
        .expect(200);

      const token = jwt.sign({ userId: verifyResponse.body.user.id }, 'test-secret');
      const response = await request(app)
        .post('/api/auth/profile')
        .set('Authorization', `Bearer ${token}`)
        .send(profilePayload)
        .expect(200);

      expect(response.body).toHaveProperty('success', true);
      expect(response.body.user).toHaveProperty('fullName', 'Aliyu Musa');
      expect(response.body.user).toHaveProperty('state', 'Gombe');
    });
  });

  describe('JWT middleware', () => {
    it('should reject requests without a token', async () => {
      await request(middlewareApp).post('/protected').expect(401);
    });

    it('should attach the decoded payload for valid tokens', async () => {
      const token = jwt.sign({ userId: 'user-123', phoneNumber: '+2348012345678' }, 'test-secret');
      const response = await request(middlewareApp)
        .post('/protected')
        .set('Authorization', `Bearer ${token}`)
        .expect(200);

      expect(response.body.user).toHaveProperty('userId', 'user-123');
    });
  });
});
