require('dotenv').config();
const express = require('express');
const cors = require('cors');
const authRoutes = require('./src/routes/authRoutes');
const productRoutes = require('./src/routes/products');
const orderRoutes = require('./src/routes/orders');
const vendorRoutes = require('./src/routes/vendors');

const app = express();
const PORT = process.env.PORT || 5000;
const IS_PRODUCTION = process.env.NODE_ENV === 'production';

// Fail fast instead of silently signing tokens with a well-known fallback.
const INSECURE_JWT_SECRETS = new Set([
  'change-me',
  'supersecretkey',
  'dev-secret-change-me',
  'secret',
  'test_secret',
]);
if (IS_PRODUCTION) {
  const secret = (process.env.JWT_SECRET || '').trim();
  if (!secret || secret.length < 16 || INSECURE_JWT_SECRETS.has(secret)) {
    console.error(
      '[config] JWT_SECRET must be set to a strong value (16+ chars) in production. ' +
        'Generate one with: openssl rand -hex 32'
    );
    process.exit(1);
  }
}

// Running behind a reverse proxy (Render, Railway, Heroku, nginx, ...) so that
// req.ip / req.protocol reflect the real client instead of the proxy.
app.set('trust proxy', 1);
app.disable('x-powered-by');

// CORS is only relevant for browser clients; the Android app sends no Origin
// header and is unaffected. Set CORS_ORIGIN to a comma-separated allowlist in
// production (e.g. "https://app.secondhandmarket.ng"). Defaults to "*".
const corsOrigins = (process.env.CORS_ORIGIN || '*')
  .split(',')
  .map((origin) => origin.trim())
  .filter(Boolean);
const allowAllOrigins = corsOrigins.includes('*');

app.use(
  cors({
    origin: allowAllOrigins
      ? '*'
      : (origin, callback) => {
          if (!origin || corsOrigins.includes(origin)) {
            callback(null, true);
          } else {
            callback(new Error('Not allowed by CORS'));
          }
        },
    credentials: !allowAllOrigins,
  })
);
app.use(express.json({ limit: '1mb' }));

// Routes
app.get('/', (req, res) => {
  res.send('Second Hand Market API is running...');
});

// Health probe used by Render/Railway and the Docker HEALTHCHECK.
app.get('/health', (req, res) => {
  res.json({
    status: 'ok',
    uptime: Math.round(process.uptime()),
    timestamp: new Date().toISOString(),
  });
});

app.use('/api/auth', authRoutes);
app.use('/api/products', productRoutes);
app.use('/api/orders', orderRoutes);
app.use('/api/vendors', vendorRoutes);

// 404 handler for unknown routes
app.use((req, res) => {
  res.status(404).json({ error: 'Route not found' });
});

// Error handling middleware
// eslint-disable-next-line no-unused-vars
app.use((err, req, res, next) => {
  console.error(err.stack);
  if (err.message === 'Not allowed by CORS') {
    return res.status(403).json({ error: 'Origin not allowed' });
  }
  res.status(500).json({ error: 'Something went wrong!' });
});

if (require.main === module) {
  const server = app.listen(PORT, () => {
    console.log(`[server] Second Hand Market API listening on port ${PORT}`);
    // eslint-disable-next-line global-require
    const { twilioConfigured, isMockMode } = require('./src/services/smsService');
    const smsMode = twilioConfigured()
      ? 'twilio (real SMS)'
      : isMockMode()
        ? 'mock (OTP returned in API response - do not use for launch)'
        : 'not configured (register returns 503)';
    console.log(`[server] OTP delivery mode: ${smsMode}`);
    if (!process.env.DATABASE_URL) {
      console.warn(
        '[server] DATABASE_URL is not set - auth falls back to the in-memory store and catalog endpoints will fail.'
      );
    }
  });

  // Create tables on first boot when a database is configured. Failures are
  // logged but do not stop the API (auth still works in mock mode).
  if (process.env.DATABASE_URL) {
    // eslint-disable-next-line global-require
    const { initDatabase } = require('./src/models/init');
    initDatabase().catch((error) => {
      console.error('[server] Database initialization failed:', error.message);
    });
  }

  const shutdown = (signal) => {
    console.log(`[server] ${signal} received - shutting down gracefully`);
    server.close(() => process.exit(0));
    // Do not hang forever if a connection keeps the server open.
    setTimeout(() => process.exit(1), 10000).unref();
  };
  ['SIGTERM', 'SIGINT'].forEach((signal) => {
    process.on(signal, () => shutdown(signal));
  });
}

module.exports = app;
