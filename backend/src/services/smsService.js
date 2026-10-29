'use strict';

/**
 * SMS delivery for OTP codes.
 *
 * Three modes:
 *  1. Twilio configured (TWILIO_ACCOUNT_SID + TWILIO_AUTH_TOKEN +
 *     TWILIO_PHONE_NUMBER) - a real SMS is sent.
 *  2. Mock mode (`NODE_ENV !== 'production'`, tests, or `ALLOW_MOCK_OTP=true`)
 *     - nothing is sent; the code is logged and the API echoes it back so the
 *     full flow can be exercised before a provider is wired up.
 *  3. Production without a provider and without `ALLOW_MOCK_OTP` - fail closed
 *     with HTTP 503 instead of handing out a code nobody can receive.
 */

class SmsNotConfiguredError extends Error {
  constructor() {
    super('SMS delivery is not configured on the server');
    this.name = 'SmsNotConfiguredError';
    this.statusCode = 503;
    this.code = 'SMS_NOT_CONFIGURED';
  }
}

function envValue(name) {
  return (process.env[name] || '').trim();
}

/** Placeholder values from .env.example must never count as real credentials. */
function isPlaceholder(value) {
  return /^(your_|changeme|dummy|test|ac_dummy)/i.test(value);
}

function twilioConfigured() {
  const sid = envValue('TWILIO_ACCOUNT_SID');
  const token = envValue('TWILIO_AUTH_TOKEN');
  const from = envValue('TWILIO_PHONE_NUMBER');
  if (!sid || !token || !from) {
    return false;
  }
  return !isPlaceholder(sid) && !isPlaceholder(token) && !isPlaceholder(from);
}

function isTestEnvironment() {
  return process.env.NODE_ENV === 'test';
}

/**
 * True when the API is allowed to echo the OTP back in the HTTP response.
 * Only for development and explicit `ALLOW_MOCK_OTP=true` deployments.
 */
function isMockMode() {
  if (isTestEnvironment() || process.env.ALLOW_MOCK_OTP === 'true') {
    return true;
  }
  return process.env.NODE_ENV !== 'production' && !twilioConfigured();
}

/**
 * Deliver a verification code to the given E.164 phone number.
 *
 * @returns {Promise<{mock: boolean}>}
 * @throws {SmsNotConfiguredError} when production has no delivery configured
 */
async function sendOtpSms(phoneNumber, otp) {
  // Never call Twilio from the test suite, even if dummy credentials are set.
  if (isTestEnvironment() || isMockMode()) {
    if (!isTestEnvironment()) {
      console.log(
        `[MOCK SMS] Code for ${phoneNumber}: ${otp} (mock mode - no SMS was sent)`
      );
    }
    return { mock: true };
  }

  if (!twilioConfigured()) {
    throw new SmsNotConfiguredError();
  }

  // Loaded lazily so mock-mode runs never initialise the SDK.
  const twilio = require('twilio');
  const client = twilio(
    envValue('TWILIO_ACCOUNT_SID'),
    envValue('TWILIO_AUTH_TOKEN')
  );

  await client.messages.create({
    body: `Your Second Hand Market verification code is ${otp}. It expires in 5 minutes. Never share this code.`,
    from: envValue('TWILIO_PHONE_NUMBER'),
    to: phoneNumber,
  });

  return { mock: false };
}

module.exports = {
  sendOtpSms,
  isMockMode,
  twilioConfigured,
  SmsNotConfiguredError,
};
