# Second Hand Market App

Buy & sell pre-loved items (laptops, phones, furniture, fashion) with OTP phone
login, vendor storefronts, bank-transfer payouts and order tracking.

- **Android app**: Kotlin + Jetpack Compose (Material 3), Hilt, Retrofit, Coil, DataStore
- **Backend**: Node.js + Express + PostgreSQL (`pg`), JWT auth
- **Repo layout**: `app/` (Android), `backend/` (API), `database/schema/` (SQL), `.github/` (CI/CD)

## Auth flow (backend + app)

1. `POST /api/auth/register` — accepts a phone number and generates an OTP.
   Outside production (or with `ALLOW_MOCK_OTP=true`) the code is returned as
   `mockOtp` and shown in the app; production without an SMS provider returns
   **503** instead of issuing an undeliverable code (fail closed).
2. `POST /api/auth/verify-otp` — mock-verifies the OTP, creates/loads the user
   and returns a **JWT** (payload: `userId`, `phoneNumber`, `role`).
3. `POST /api/auth/profile` — **JWT-protected**; accepts `name`, `state`,
   `lga`, `address` and updates the user record.

Android navigation mirrors this: `landing` → `otp_verification/{phone}` →
`profile_setup` → `dashboard`.

If the API server is unreachable, **debug** builds fall back to a demo mode
(OTP `123456`, locally persisted profile) so the UI flow never dead-ends.
Release builds show a connection error and never accept the demo code.

## Backend quick start

```bash
cd backend
cp .env.example .env          # set DATABASE_URL + JWT_SECRET
npm install
npm run dev                   # http://localhost:5000
```

Endpoints: `/api/auth/*`, `/api/products/*`, `/api/orders/*`, `/api/vendors/*`.

Tests & lint: `npm test` (Jest + supertest, DB is mocked) · `npm run lint`.

## Android quick start

Open the repo root in Android Studio (or `./gradlew :app:assembleDebug`).
The API base URL is baked into the APK at build time as
`BuildConfig.API_BASE_URL`. Override it in `local.properties`
(`API_BASE_URL=http://10.0.2.2:5000/` for the emulator → host loopback), on the
command line with `-PAPI_BASE_URL=...`, or with the GitHub Actions
`API_BASE_URL` repository variable — see `DEPLOYMENT.md` section 9.

Demo path: enter any phone number (`+234XXXXXXXXXX`) → Continue → OTP →
profile setup → dashboard (User/Vendor mode toggle in the top bar).

## Deployment

The backend ships as a Docker image. `DEPLOYMENT.md` covers deployment on the
GitHub Student credit (DigitalOcean App Platform, always on), Render, Railway
and Azure for Students, plus Neon Postgres, Twilio and pointing the APK at the
live server. After deploying, verify it with:

```bash
./scripts/verify-api.sh https://your-app.ondigitalocean.app
```

## Notes

- Database models live in `backend/src/models/` (they also `CREATE TABLE IF
  NOT EXISTS` via `src/models/init.js`); `database/schema/*.sql` mirrors them
  for manual provisioning.
- CI: Android build/test/lint, backend test/lint, code quality, release.
