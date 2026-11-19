# Deployment Guide

How to deploy the **Second Hand Market** backend and point the Android app at
the live server — without changing any Kotlin code.

- Backend: `backend/` (Node.js + Express + PostgreSQL + Twilio SMS)
- Android: `app/` (API base URL is baked in at build time via `BuildConfig.API_BASE_URL`)

The backend is a standard container (`backend/Dockerfile`), so any Docker host
works. Section 2 is the recommended path for GitHub Student Developer Pack
members; sections 3 and 7 cover free alternatives.

---

## 1. Backend environment variables

| Variable | Required | Description |
| --- | --- | --- |
| `NODE_ENV` | yes | `production` on the live server. Enables fail-closed security checks. |
| `PORT` | no | Injected by most platforms. Defaults to `5000`. |
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
configure Twilio (section 5) and remove it.

> **Important:** the app has no client-side fallback when the server *answers*
> with an error. A reachable production API with no SMS provider rejects every
> registration with HTTP 503. That is why testing needs either Twilio or
> `ALLOW_MOCK_OTP=true`.

---

## 2. Deploy on the GitHub Student credit (DigitalOcean App Platform) — recommended

The GitHub Student Developer Pack includes **$200 of DigitalOcean credit**.
App Platform runs the container 24/7 (no sleep) and provides a managed HTTPS
URL on `*.ondigitalocean.app`.

| Item | Value |
| --- | --- |
| Plan | `apps-s-1vcpu-0.5gb` — $5/month (512 MiB RAM) |
| Always-on | yes, no cold starts |
| HTTPS | managed certificate, `https://<app>-<id>.ondigitalocean.app` |
| Deploys | automatic on every push to `main` |
| Database | free Neon Postgres (recommended) to stretch the credit |

The repository contains a ready app spec at `deploy/digitalocean/app.yaml`.

### 2.1 Claim the credit

1. Open [github.com/settings/edu](https://github.com/settings/edu) →
   **Student Developer Pack** → **DigitalOcean** → **Get access**.
2. Create/verify a DigitalOcean account. DigitalOcean asks for a payment method
   for identity verification; the $200 credit covers usage, it is not a charge.
3. Confirm the credit appears under **Billing → Credits**.

> If you cannot use a card with DigitalOcean, use the Azure for Students path
> in section 7 instead — it does not require one.

### 2.2 Deploy the committed app spec

1. Create an API token: DigitalOcean Control Panel → **API** → **Tokens** →
   **Generate New Token** with read + write scope. Copy it.
2. Install `doctl` using one of:
   - `snap install doctl`
   - `brew install doctl`
   - a release binary from
     [github.com/digitalocean/doctl/releases](https://github.com/digitalocean/doctl/releases)
3. Authenticate and check access:
   ```bash
   doctl auth init          # paste the token
   doctl account get
   ```
4. Create the app with a generated JWT secret **without editing the tracked
   file** (so the secret never reaches git):
   ```bash
   SECRET="$(openssl rand -hex 32)"
   sed "s/CHANGE_ME/$SECRET/" deploy/digitalocean/app.yaml > /tmp/shm-app.yaml
   doctl apps create --spec /tmp/shm-app.yaml --wait
   rm /tmp/shm-app.yaml
   ```
   The API deliberately refuses to boot with `CHANGE_ME`, so a skipped step
   appears as a clear deploy-log error instead of an insecure token signer.
   The first attempt may stop because DigitalOcean's GitHub app is not
   installed. Open the printed link, grant access to
   `Prudent66/Second-Hand-Market-App`, then run the command again.
5. Copy the public URL:
   ```bash
   doctl apps list
   doctl apps spec get <APP_ID> | grep -A3 domains
   ```
   It looks like `https://second-hand-market-api-abc12.ondigitalocean.app`.

After the first deploy, change environment variables in the dashboard or in a
local copy of the live spec — **not** in the tracked file:

```bash
doctl apps spec get <APP_ID> > /tmp/shm-app-live.yaml
# edit /tmp/shm-app-live.yaml, then:
doctl apps update <APP_ID> --spec /tmp/shm-app-live.yaml --wait
```

Re-applying `deploy/digitalocean/app.yaml` directly would reset `JWT_SECRET` to
the `CHANGE_ME` placeholder and the API would refuse to boot.

To deploy without any spec change (e.g. after editing env vars in the UI):

```bash
doctl apps create-deployment <APP_ID> --wait
```

### 2.3 First-run settings

- The spec starts with `ALLOW_MOCK_OTP=true` so you can sign in **without
  Twilio**: `/api/auth/register` returns `mockOtp` and the Android app shows it
  in the "Demo mode" hint. **While this is on, anyone with the URL can sign in
  as any phone number.** Switch it off (section 5) before sharing the app or
  storing real user data.
- `DATABASE_URL` is intentionally absent, so the API boots in in-memory mode
  and accounts disappear on every restart or redeploy. Add a Neon URL
  (section 4) for persistence.
- To change values: **Apps → second-hand-market-api → api → Settings →
  Environment Variables**, mark secrets as **Encrypted**, then **Save** (this
  redeploys the app).

### 2.4 Dashboard-only alternative

Apps → **Create App** → GitHub → authorize → pick the repo and `main` → edit
the service: **Source Directory** `backend`, **Dockerfile** build, **HTTP
Port** `5000`, instance size **Basic ($5)**, health check `/health`, region
London or Frankfurt. Add the variables from section 1, then **Create**.

### Cost notes

$5/month divides the $200 credit into 40 months, but the student credit is
valid for 12 months from claiming (see **Billing → Credits**), so treat the
first year as free rather than permanent. A free Neon database keeps the total
at $5/month. One container is also a hard requirement right now: OTPs are kept
in memory (section 11).

---

## 3. Deploy to Render (free alternative)

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
> to wake it, or move to the student-credit plan in section 2 for an
> always-on server.

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

## 4. Database (PostgreSQL)

Authentication falls back to an in-memory store, but products, vendors and
orders need PostgreSQL. Tables are created automatically on first boot.

**Neon (free, recommended):**

1. Create a project at [neon.tech](https://neon.tech).
2. Copy the connection string (it includes `?sslmode=require`).
3. Add it as `DATABASE_URL` on your host, mark it as a secret, and redeploy.
4. On App Platform the API connects with SSL automatically in production.

**DigitalOcean managed PostgreSQL:** **Create → Databases → PostgreSQL**, pick
the smallest dev plan (~$7/month, also covered by the student credit), then use
its connection string as `DATABASE_URL`. Neon is free and leaves more credit for
the API container.

**Render PostgreSQL:** Dashboard → **New +** → **Postgres**, then copy the
connection string into `DATABASE_URL`.

---

## 5. Twilio (real SMS OTP)

1. Create an account at [twilio.com](https://www.twilio.com) and verify your
   own phone number (trial accounts can only text verified numbers).
2. From the Twilio Console, copy **Account SID** and **Auth Token**.
3. Buy/acquire a phone number with SMS capability — that is
   `TWILIO_PHONE_NUMBER` in E.164 (e.g. `+12025550123`).
4. Add all three variables to the server environment.
5. Set `ALLOW_MOCK_OTP` to `false` (or delete it) and redeploy.
6. Test with `scripts/verify-api.sh` (section 8). The response must **not**
   contain `mockOtp`, and your phone should receive the SMS.

> Twilio trial accounts only deliver to numbers verified in the console, which
> is fine for testing your own phone. Sending to arbitrary Nigerian numbers
> requires upgrading and, depending on the route, a sender ID.

---

## 6. Deploy to Railway (alternative)

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

## 7. Deploy on Azure for Students (fallback)

Azure for Students gives **$100 for 12 months with no credit card**, but
requires its own student verification (school email/SheerID; GitHub Education
verification alone does not guarantee it). The cheapest sensible always-on
option is a Linux **App Service B1** plan at roughly $13/month — about seven
months of credit.

```bash
az login
RG=shm-rg
APP="second-hand-market-api-$RANDOM"

az group create -n "$RG" -l westeurope
az appservice plan create -n shm-plan -g "$RG" --is-linux --sku B1
az webapp create -n "$APP" -g "$RG" --plan shm-plan --runtime "NODE:20-lts"

# Always On prevents the 20-minute idle unload, so there are no cold starts.
az webapp config set -n "$APP" -g "$RG" --always-on true --startup-file "npm start"

az webapp config appsettings set -n "$APP" -g "$RG" --settings \
  NODE_ENV=production \
  SCM_DO_BUILD_DURING_DEPLOYMENT=true \
  JWT_SECRET="$(openssl rand -hex 32)" \
  ALLOW_MOCK_OTP=true

# Zip-deploy the backend; App Service runs npm install and npm start.
az webapp deploy -n "$APP" -g "$RG" --src-path backend --type zip
```

The URL is `https://$APP.azurewebsites.net`. Add `DATABASE_URL` and the Twilio
variables under **Configuration → Application settings**, then restart.

Notes:

- If Azure rejects the runtime name, list the current ones with
  `az webapp list-runtimes --os linux | grep NODE`.
- App Service sets `PORT` itself; do not override it.
- Container Apps (`az containerapp up --source backend`) is another option, but
  scale-to-zero reintroduces cold starts and always-on replicas cost more than
  App Service B1.

---

## 8. Verify the deployment

Fastest check (health + OTP + token in one go):

```bash
./scripts/verify-api.sh https://YOUR-SERVICE-URL
```

The script exits non-zero on failure and explains the common causes. Manual
equivalent:

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

A `503 SMS delivery is not configured` response is the fail-closed behaviour
described in section 1 — enable mock OTP or add Twilio credentials.

---

## 9. Point the Android app at the live server

The URL is baked into the APK at build time as `BuildConfig.API_BASE_URL`.
**No Kotlin changes required.** Pick one of these:

| Where | How | Use case |
| --- | --- | --- |
| GitHub Actions | Repo → **Settings → Secrets and variables → Actions → Variables** → new variable `API_BASE_URL` | CI-built APK artifact points at production (recommended) |
| `gradle.properties` (committed) | `API_BASE_URL=https://your-app.ondigitalocean.app/` | Permanent default for everyone building the repo |
| `local.properties` (gitignored) | add `API_BASE_URL=http://10.0.2.2:5000/` | Your machine only (e.g. emulator + local backend) |
| Command line | `./gradlew assembleDebug -PAPI_BASE_URL=https://your-app.ondigitalocean.app/` | One-off build |

Resolution order: command line → `local.properties` → `gradle.properties` → built-in default.
A trailing `/` is added automatically if you forget it.

> The CI workflow already forwards `vars.API_BASE_URL` into Gradle. After
> setting that repository variable, push any commit (or re-run the Android CI
> workflow) and install the new `debug-apk` artifact — the old APK keeps the
> old URL.

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

## 10. Production checklist

- [ ] `JWT_SECRET` is a random 16+ character value (server refuses weak/missing secrets in production)
- [ ] `ALLOW_MOCK_OTP` is removed or `false`
- [ ] Twilio credentials set and the sender number can send SMS
- [ ] `DATABASE_URL` points at a persistent PostgreSQL database
- [ ] `CORS_ORIGIN` restricted if a web client is added (native app unaffected)
- [ ] HTTPS only (App Platform, Render, Railway and Azure provide certificates automatically)
- [ ] `.env` never committed
- [ ] `API_BASE_URL` updated in the GitHub Actions variable (or `gradle.properties`), and a new APK built
- [ ] `./scripts/verify-api.sh <url>` passes against the production URL
- [ ] Platform is always-on (App Platform/Azure) or you accept Render cold starts
- [ ] Student credit expiry date noted; billing alerts enabled

---

## 11. Known limitations

- **The offline demo fallback is debug-only.** Debug APKs fall back to the
  local demo code `123456` only when the server is *unreachable*. A reachable
  API that returns 503/500 does not trigger the fallback — fix the server
  configuration. Release builds never accept the demo code.
- **OTPs are stored in memory.** Restarting or redeploying the server
  invalidates pending codes (users simply request a new one). Running more than
  one instance requires moving the OTP store to PostgreSQL or Redis. App
  Platform auto-deploys on every push to `main`, which restarts the single
  instance.
- **Render free tier sleeps.** App Platform and Azure App Service (Always On)
  do not.
- **No rate limiting yet.** Add `express-rate-limit` on `/api/auth/*` before
  opening the API to the public.
