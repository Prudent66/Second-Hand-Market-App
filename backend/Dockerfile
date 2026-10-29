# ---------- Dependency stage ----------
FROM node:20-alpine AS deps

WORKDIR /app

# Install only production dependencies from the lockfile for reproducible builds
COPY package.json package-lock.json ./
RUN npm ci --omit=dev && npm cache clean --force

# ---------- Runtime stage ----------
FROM node:20-alpine

ENV NODE_ENV=production \
    PORT=5000

WORKDIR /app

# Run as a non-root user
RUN addgroup -S app && adduser -S app -G app

COPY --from=deps --chown=app:app /app/node_modules ./node_modules
COPY --chown=app:app package.json package-lock.json ./
COPY --chown=app:app server.js ./
COPY --chown=app:app src ./src

USER app

EXPOSE 5000

HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
  CMD wget -qO- http://127.0.0.1:5000/health > /dev/null 2>&1 || exit 1

CMD ["node", "server.js"]
