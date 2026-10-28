# Second Hand Market App

Buy & sell pre-loved items (laptops, phones, furniture, fashion) with OTP phone
login, vendor storefronts, bank-transfer payouts and order tracking.

- **Android app**: Kotlin + Jetpack Compose (Material 3), Hilt, Retrofit, Coil, DataStore
- **Backend**: Node.js + Express + PostgreSQL (`pg`), JWT auth
- **Repo layout**: `app/` (Android), `backend/` (API), `database/schema/` (SQL), `.github/` (CI/CD)

## Auth flow (backend + app)

1. `POST /api/auth/register` — accepts a phone number, generates a **mock OTP**
   (returned as `mockOtp` outside production; the real one is logged as
   `[MOCK SMS]`). Swap in Twilio/Firebase Phone Auth later.
2. `POST /api/auth/verify-otp` — mock-verifies the OTP, creates/loads the user
   and returns a **JWT** (payload: `userId`, `phoneNumber`, `role`).
3. `POST /api/auth/profile` — **JWT-protected**; accepts `name`, `state`,
   `lga`, `address` and updates the user record.

Android navigation mirrors this: `landing` → `otp_verification/{phone}` →
`profile_setup` → `dashboard`.

If the API server is unreachable, the app falls back to a demo mode
(OTP `123456`, locally persisted profile) so the UI flow never dead-ends.

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
The app expects the API at `http://10.0.2.2:5000/` (emulator → host loopback);
configure it in `app/src/main/java/com/secondhandmarket/di/NetworkModule.kt`.

Demo path: enter any phone number (`+234XXXXXXXXXX`) → Continue → OTP →
profile setup → dashboard (User/Vendor mode toggle in the top bar).

## Notes

- Database models live in `backend/src/models/` (they also `CREATE TABLE IF
  NOT EXISTS` via `src/models/init.js`); `database/schema/*.sql` mirrors them
  for manual provisioning.
- CI: Android build/test/lint, backend test/lint, code quality, release.
