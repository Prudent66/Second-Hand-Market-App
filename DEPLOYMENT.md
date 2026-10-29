# Deployment Guide

How to deploy the **Second Hand Market** backend and point the Android app at
the live server — without changing any Kotlin code.

- Backend: `backend/` (Node.js + Express + PostgreSQL + Twilio SMS)
- Android: `app/` (API base URL is baked in at build time via `BuildConfig.API_BASE_URL`)

---

## 1. Backend environment variables

| Variable | Required | Description |
| --- | --- | --- |
| `NODE_ENV` | yes | `production` on the live server. Enables fail-closed security checks. |
| `PORT` | no | Injected by Render/Railway. Defaults to `5000`. |
| `JWT_SECRET` | **yes** | 16+ random chars. The server refuses to start in production without it. Generate: `openssl rand -hex 32`. |
| `DATABASE_URL` | recommended | PostgreSQL connection string. The server auto-creates tables on boot. Without it, auth uses an in-memory fallback and catalog/orders endpoints fail. |
| `TWILIO_ACCOUNT_SID` | for real SMS | Twilio Account SID. |
| `TWILIO_AUTH_TOKEN` | for real SMS | Twilio Auth Token. |
| `TWILIO_PHONE_NUMBER` | for real SMS | Twilio sender number in E.164, e.g. `+12025550123`. |
| `ALLOW_MOCK_OTP` | testing only | `true` lets the API return the OTP in the register response (and log it) instead of sending an SMS. **Set to `false`/remove before launching.** |
| `CORS_ORIGIN` | no | Comma-separated browser origins allowed by CORS, or `*` (default). The Android app sends no `Origin` header, so this only affects web clients. |

### OTP delivery modes

| Situation | Behaviour |
| --- | --- |
| Twilio vars set | Real SMS is sent. The OTP is **never** returned in the response. |
| `NODE_ENV != production`, or `ALLOW_MOCK_OTP=true` | No SMS. The code is logged and returned as `mockOtp`, and the Android app shows a tappable "Demo mode" hint. |
| `NODE_ENV=production` and no Twilio, no `ALLOW_MOCK_OTP` | Register returns **503 SMS delivery is not configured** (fail closed). |

That last rule is deliberate: production must never hand out a code that cannot
be delivered. Use `ALLOW_MOCK_OTP=true` for the first deployment test, then
configure Twilio and remove it.

---

## 2. Deploy to Render (recommended)

### Option A — Blueprint (one click)

The repo contains `render.yaml`, which describes the web service.

1. Push this repository to GitHub (already done).
2. Open [dashboard.render.com](https://dashboard.render.com) → **New +** → **Blueprint**.
3. Connect the repository and click **Apply**.
4. Render creates the service **`second-hand-market-api`** with:
   - root directory `backend`
   - build command `npm ci --omit=dev`
   - start command `npm start`
   - health check `/health`
   - a generated `JWT_SECRET`
5. When prompted, paste optional values (database URL, Twilio keys) — you can
   also add them later under **Environment**.
6. Wait for the first deploy to finish, then copy the service URL:
   `https://second-hand-market-api.onrender.com`

> **Free plan note:** Render free web services sleep after ~15 minutes of
> inactivity. The next request takes 30–60 s to wake the server, which can be
> longer than the app's 15 s connect timeout — open the URL in a browser first
> to wake it, or upgrade the plan for production.

### Option B — Manual web service

1. **New +** → **Web Service** → connect the repo.
2. Settings:
   - **Root Directory:** `backend`
   - **Runtime:** Node
   - **Build Command:** `npm ci --omit=dev`
   - **Start Command:** `npm start`
   - **Health Check Path:** `/health`
3. Add the environment variables from the table in section 1.

---

## 3. Database (PostgreSQL)

Authentication falls back to an in-memory store, but products, vendors and
orders need PostgreSQL. Tables are created automatically on first boot.

**Neon (free, recommended):**

1. Create a project at [neon.tech](https://neon.tech).
2. Copy the connection string (it includes `?sslmode=require`).
3. Add it as `DATABASE_URL` on Render/Railway and redeploy.

**Render PostgreSQL:** Dashboard → **New +** → **Postgres**, then copy the
connection string into `DATABASE_URL`.

---

## 4. Twilio (real SMS OTP)

1. Create an account at [twilio.com](https://www.twilio.com) and verify your
   own phone number (trial accounts can only text verified numbers).
2. From the Twilio Console, copy **Account SID** and **Auth Token**.
3. Buy/acquire a phone number with SMS capability — that is
   `TWILIO_PHONE_NUMBER` in E.164 (e.g. `+12025550123`).
4. Add all three variables to the server environment.
5. Set `ALLOW_MOCK_OTP` to `false` (or delete it) and redeploy.
6. Test with `curl` (section 6). The response must **not** contain `mockOtp`,
   and your phone should receive the SMS.

---

## 5. Deploy to Railway (alternative)

1. [railway.app](https://railway.app) → **New Project** → **Deploy from GitHub repo**.
2. Open the service → **Settings**:
   - **Root Directory:** `backend`
   - Railway detects the `Dockerfile` and/or Node runtime automatically.
   - **Healthcheck Path:** `/health`
3. **Variables** → add the table from section 1.
4. **Settings → Networking → Generate Domain**, then copy the URL
   (e.g. `https://second-hand-market-api.up.railway.app`).

CLI alternative: `npm i -g @railway/cli && railway login && railway up` from `backend/`.

---

## 6. Verify the deployment

```bash
export API=https://YOUR-SERVICE-URL          # no trailing slash needed

# 1. Health probe -> {"status":"ok",...}
curl "$API/health"

# 2. Request an OTP
curl -X POST "$API/api/auth/register" \
  -H 'Content-Type: application/json' \
  -d '{"phoneNumber":"+2348012345678"}'
#  -> with ALLOW_MOCK_OTP=true: {"success":true,...,"mockOtp":"123456"}
#  -> with Twilio configured:   {"success":true,"message":"OTP sent successfully"}

# 3. Verify it (use the mockOtp from step 2, or the SMS you received)
curl -X POST "$API/api/auth/verify-otp" \
  -H 'Content-Type: application/json' \
  -d '{"phoneNumber":"+2348012345678","otp":"123456"}'
#  -> {"success":true,"token":"eyJ...","user":{...}}
```

---

## 7. Point the Android app at the live server

The URL is baked into the APK at build time as `BuildConfig.API_BASE_URL`.
**No Kotlin changes required.** Pick one of these:

| Where | How | Use case |
| --- | --- | --- |
| `gradle.properties` (committed) | `API_BASE_URL=https://your-api.onrender.com/` | Permanent default for everyone building the repo |
| `local.properties` (gitignored) | add `API_BASE_URL=http://10.0.2.2:5000/` | Your machine only (e.g. emulator + local backend) |
| Command line | `./gradlew assembleDebug -PAPI_BASE_URL=https://your-api.onrender.com/` | One-off build |
| GitHub Actions | Repo → **Settings → Secrets and variables → Actions → Variables** → new variable `API_BASE_URL` | CI-built APK artifact points at production |

Resolution order: command line → `local.properties` → `gradle.properties` → built-in default.
A trailing `/` is added automatically if you forget it.

> The CI workflow already forwards `vars.API_BASE_URL` into Gradle. Set that
> repository variable and every APK built by GitHub Actions will talk to your
> live backend — then install the `debug-apk` artifact as usual.

To confirm what a build uses: `app/build/generated/source/buildConfig/debug/com/secondhandmarket/BuildConfig.java`
contains the resolved `API_BASE_URL`.

### Local development against a local backend

```bash
# backend
cd backend && npm start            # http://localhost:5000

# Android: local.properties
echo "API_BASE_URL=http://10.0.2.2:5000/" >> local.properties   # emulator -> host machine
```

Cleartext HTTP is only allowed for `localhost` and `10.0.2.2`
(`app/src/main/res/xml/network_security_config.xml`). All other hosts must use HTTPS.

---

## 8. Production checklist

- [ ] `JWT_SECRET` is a random 16+ character value (server refuses weak/missing secrets in production)
- [ ] `ALLOW_MOCK_OTP` is removed or `false`
- [ ] Twilio credentials set and the sender number can send SMS
- [ ] `DATABASE_URL` points at a persistent PostgreSQL database
- [ ] `CORS_ORIGIN` restricted if a web client is added (native app unaffected)
- [ ] HTTPS only (Render/Railway provide certificates automatically)
- [ ] `.env` never committed
- [ ] `API_BASE_URL` updated in `gradle.properties` or the GitHub Actions variable, and a new APK built
- [ ] Run the curl checks in section 6 against the production URL

## 9. Known limitations

- **The offline demo fallback is debug-only.** Debug APKs fall back to the
  local demo code `123456` when the server is unreachable (including when a
  Render free instance is asleep), so wake the service (open `/health` in a
  browser) before testing the real SMS flow. Release builds show a connection
  error instead and never accept the demo code.
- **OTPs are stored in memory.** Restarting or redeploying the server
  invalidates pending codes (users simply request a new one). Running more than
  one instance requires moving the OTP store to PostgreSQL or Redis.
- **Render free tier sleeps.** Wake the service before demoing the app.
- **No rate limiting yet.** Add `express-rate-limit` on `/api/auth/*` before
  opening the API to the public.
