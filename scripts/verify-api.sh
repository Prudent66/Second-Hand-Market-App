#!/usr/bin/env bash
#
# Smoke-test a deployed Second Hand Market API.
#
#   ./scripts/verify-api.sh https://your-app.ondigitalocean.app
#   PHONE=+2348012345678 ./scripts/verify-api.sh https://your-app.ondigitalocean.app
#
# Checks /health, requests an OTP and (when the server runs with
# ALLOW_MOCK_OTP=true) verifies it, printing the issued JWT length.
# Exits non-zero on failure. Uses curl + sed only.

set -euo pipefail

BASE="${1:-}"
if [[ -z "$BASE" ]]; then
  echo "usage: $0 <api-base-url>" >&2
  exit 2
fi
BASE="${BASE%/}"
PHONE="${PHONE:-+2348000000000}"

echo "==> GET $BASE/health"
HEALTH="$(curl -fsS --max-time 20 "$BASE/health")" || {
  echo "FAIL: /health unreachable (server down, still deploying, or cold start)" >&2
  exit 1
}
echo "$HEALTH"
if [[ "$HEALTH" != *'"status":"ok"'* ]]; then
  echo "FAIL: unexpected /health payload" >&2
  exit 1
fi

echo
echo "==> POST $BASE/api/auth/register ($PHONE)"
REGISTER="$(curl -sS --max-time 20 -X POST "$BASE/api/auth/register" \
  -H 'Content-Type: application/json' \
  -d "{\"phoneNumber\":\"$PHONE\"}")"
echo "$REGISTER"

OTP="$(printf '%s' "$REGISTER" \
  | sed -n 's/.*"mockOtp"[[:space:]]*:[[:space:]]*"\([0-9]\{4,8\}\)".*/\1/p')"

if [[ -z "$OTP" ]]; then
  echo
  echo "No mockOtp in the response."
  if [[ "$REGISTER" == *'SMS delivery is not configured'* ]]; then
    echo "FAIL: production API has no Twilio credentials." >&2
    echo "      For testing, set ALLOW_MOCK_OTP=true and redeploy;" >&2
    echo "      for production, configure Twilio (DEPLOYMENT.md section 5)." >&2
    exit 1
  fi
  echo "If Twilio is configured this is expected: check the phone for the SMS."
  exit 0
fi

echo
echo "mock OTP: $OTP  (ALLOW_MOCK_OTP is ON - disable after testing)"
echo
echo "==> POST $BASE/api/auth/verify-otp"
VERIFY="$(curl -sS --max-time 20 -X POST "$BASE/api/auth/verify-otp" \
  -H 'Content-Type: application/json' \
  -d "{\"phoneNumber\":\"$PHONE\",\"otp\":\"$OTP\"}")"

TOKEN="$(printf '%s' "$VERIFY" \
  | sed -n 's/.*"token"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p')"

if [[ -z "$TOKEN" ]]; then
  echo "$VERIFY"
  echo "FAIL: no token returned by /verify-otp" >&2
  exit 1
fi

echo "OK: signed in, JWT length ${#TOKEN}"
echo
echo "Deployment looks healthy. Point the app at: $BASE"
