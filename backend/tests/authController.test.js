const request = require('supertest');
const express = require('express');
const jwt = require('jsonwebtoken');

// Mock the database
jest.mock('../src/config/db', () => ({
  query: jest.fn()
}));

const db = require('../src/config/db');
const authController = require('../src/controllers/authController');

const app = express();
app.use(express.json());
app.post('/api/auth/send-otp', authController.sendOtp);
app.post('/api/auth/verify-otp', authController.verifyOtp);

describe('Auth Controller', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    process.env.JWT_SECRET = 'test-secret';
  });

  describe('POST /api/auth/send-otp', () => {
    it('should send OTP and return mock OTP in development', async () => {
      const response = await request(app)
        .post('/api/auth/send-otp')
        .send({ phoneNumber: '+2348012345678' })
        .expect(200);

      expect(response.body).toHaveProperty('message', 'OTP sent successfully');
      expect(response.body).toHaveProperty('mockOtp');
      expect(response.body.mockOtp).toMatch(/^\d{6}$/);
    });

    it('should return 400 for invalid phone number', async () => {
      const response = await request(app)
        .post('/api/auth/send-otp')
        .send({ phoneNumber: 'invalid' })
        .expect(400);

      expect(response.body).toHaveProperty('error');
    });
  });

  describe('POST /api/auth/verify-otp', () => {
    it('should verify OTP and return JWT token', async () => {
      // First send OTP
      const sendResponse = await request(app)
        .post('/api/auth/send-otp')
        .send({ phoneNumber: '+2348012345678' });

      const mockOtp = sendResponse.body.mockOtp;

      // Mock database to return existing user
      db.query.mockResolvedValueOnce({
        rows: [{ id: 'user-123', phone_number: '+2348012345678', full_name: 'Test User', is_vendor: false }]
      });

      const response = await request(app)
        .post('/api/auth/verify-otp')
        .send({ phoneNumber: '+2348012345678', otp: mockOtp })
        .expect(200);

      expect(response.body).toHaveProperty('token');
      expect(response.body.user).toHaveProperty('id', 'user-123');
      expect(response.body.user).toHaveProperty('phoneNumber', '+2348012345678');

      // Verify JWT token
      const decoded = jwt.verify(response.body.token, process.env.JWT_SECRET);
      expect(decoded.userId).toBe('user-123');
    });

    it('should return 400 for invalid OTP', async () => {
      const response = await request(app)
        .post('/api/auth/verify-otp')
        .send({ phoneNumber: '+2348012345678', otp: '000000' })
        .expect(400);

      expect(response.body).toHaveProperty('error', 'Invalid OTP');
    });

    it('should create new user if not exists', async () => {
      // Send OTP first
      const sendResponse = await request(app)
        .post('/api/auth/send-otp')
        .send({ phoneNumber: '+2349012345678' });

      const mockOtp = sendResponse.body.mockOtp;

      // Mock database to return empty (new user)
      db.query.mockResolvedValueOnce({ rows: [] });
      db.query.mockResolvedValueOnce({
        rows: [{ id: 'new-user-456', phone_number: '+2349012345678', full_name: null, is_vendor: false }]
      });

      const response = await request(app)
        .post('/api/auth/verify-otp')
        .send({ phoneNumber: '+2349012345678', otp: mockOtp })
        .expect(200);

      expect(response.body.user.id).toBe('new-user-456');
    });
  });
});