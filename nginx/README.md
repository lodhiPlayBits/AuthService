# Complete Guide — Running Nginx Gateway with Auth Service

This guide walks you through starting the Nginx (OpenResty + LuaJIT) gateway
and connecting it to your Spring Boot auth service.

---

## Architecture Overview

```
Internet
   │
   ▼
┌──────────────────────────────────────────────────┐
│  Nginx Gateway (OpenResty + LuaJIT)              │
│  ┌────────────┐ ┌──────────┐ ┌────────────────┐  │
│  │ Rate Limit │ │   CORS   │ │ Validate Req   │  │
│  │  (nginx)   │ │  (Lua)   │ │    (Lua)       │  │
│  └────────────┘ └──────────┘ └────────────────┘  │
│            │            │            │            │
│            └────────────┴────────────┘            │
│                         │                         │
│              proxy_pass to auth_backend           │
│                    :80 / :443                     │
└──────────────────────┬───────────────────────────┘
                       │  Docker network: authnet
                       ▼
┌──────────────────────────────────────────────────┐
│  Auth Service (Spring Boot)                      │
│            :8080 (internal only)                 │
└──────────────────────────────────────────────────┘
```

Both services run in separate `docker compose` stacks but share the
same Docker network (`authnet`), so nginx reaches auth-service by
container name.

---

## Prerequisites

- Docker Engine running (via WSL on Windows)
- Docker Compose v2+
- Your auth-service `.env` file configured (database, JWT, etc.)

---

## Step 1 — Create the Shared Docker Network

Both compose stacks reference `authnet` as an external network.
Create it once — it persists across restarts:

```powershell
wsl docker network create authnet
```

> **Verify:** `wsl docker network ls` — you should see `authnet` in the list.

---

## Step 2 — Configure Your Domain

### Option A: Local Development (use `localhost`)

Edit [`nginx/conf.d/auth-service.conf`](file:///e:/auth/nginx/conf.d/auth-service.conf) —
replace `yourdomain.com` with `localhost`:

```nginx
# Line 4 and 19 — change both server_name entries:
server_name localhost;

# Line 21-22 — change cert paths:
ssl_certificate     /etc/nginx/ssl/live/localhost/fullchain.pem;
ssl_certificate_key /etc/nginx/ssl/live/localhost/privkey.pem;
```

### Option B: Production (use your real domain)

Replace `yourdomain.com` with your actual domain (e.g. `api.lodhi.com`)
in these files:

| File | Lines to edit |
|------|---------------|
| [`nginx/conf.d/auth-service.conf`](file:///e:/auth/nginx/conf.d/auth-service.conf) | `server_name` (optional, can leave as `_`), `ssl_certificate`, `ssl_certificate_key` |
| [`nginx/init-letsencrypt.sh`](file:///e:/auth/nginx/init-letsencrypt.sh) | `DOMAIN=` and `EMAIL=` at the top |

### Option C: AWS / No Domain (IP Address Only)

If you are running on AWS EC2 and **don't have a domain yet**, I have already configured `auth-service.conf` with `server_name _;` (a catch-all) for you. 

1. Simply leave `DOMAIN=yourdomain.com` in your `.env` file (or don't set it).
2. Nginx will generate a self-signed certificate for `yourdomain.com` automatically on boot.
3. You can access your API directly using your EC2 Public IP: `https://<YOUR-EC2-PUBLIC-IP>/api/...`
4. *Note:* Your browser/client will show a security warning because the certificate is self-signed. Just proceed/accept the risk until you buy a domain later. When you get a domain, follow Option B.

---

## Step 3 — Start Auth Service

```powershell
# From the auth_service directory
cd e:\auth\auth_service

# Local development
wsl docker compose --env-file .env up -d --build

# OR Production
wsl docker compose --env-file .env.prod up -d --build
```

> **Verify:** `wsl docker ps` — you should see `auth-service` running and healthy.

---

## Step 4 — Start Nginx Gateway

```powershell
# From the nginx directory
cd e:\auth\nginx

# Build and start
wsl docker compose up -d --build
```

**What happens on first start:**
1. Dockerfile builds the OpenResty image with your configs + Lua scripts baked in
2. Entrypoint detects no SSL certs exist → generates self-signed certs automatically
3. Generates 2048-bit DH parameters (takes ~30 seconds)
4. Starts OpenResty on ports 80 and 443

> **Verify:**
> ```powershell
> # Check containers are running
> wsl docker ps
>
> # Test health endpoint (use -k to accept self-signed cert)
> wsl curl -kI https://localhost/health
>
> # Check nginx config is valid
> wsl docker exec nginx-gateway openresty -t
> ```

---

## Step 5 — Test the API Through Nginx

All requests now go through the gateway on port 443 (HTTPS) or 80 (redirects to HTTPS).

### Register a new user
```powershell
wsl curl -k -X POST https://localhost/api/auth/register `
  -H "Content-Type: application/json" `
  -d '{"username":"testuser","email":"test@example.com","password":"SecurePass123!"}'
```

### Login
```powershell
wsl curl -k -X POST https://localhost/api/auth/login `
  -H "Content-Type: application/json" `
  -d '{"username":"testuser","password":"SecurePass123!"}'
```

### Access a protected endpoint (with token)
```powershell
wsl curl -k https://localhost/api/some-protected-endpoint `
  -H "Authorization: Bearer YOUR_JWT_TOKEN_HERE"
```

### What the gateway does on each request:
1. **Rate limiting** — 3 req/s for login/register, 10 req/s for general API
2. **CORS** — handles OPTIONS preflight, sets Access-Control headers
3. **X-Request-ID** — injects a unique ID for tracing (or forwards the client's)
4. **Content-Type check** — rejects POST/PUT/PATCH without `application/json`
5. **Bearer token check** — rejects protected routes without `Authorization: Bearer <token>` (format check only — Spring Security does real validation)
6. **Security headers** — HSTS, X-Frame-Options, X-Content-Type-Options, etc.
7. **Proxy** — forwards to `auth-service:8080` on the internal Docker network

---

## Step 6 — Production: Get Real SSL Certs (Let's Encrypt)

> Skip this for local development — self-signed certs are fine.

1. Point your domain's DNS A record to your server's public IP
2. Edit [`init-letsencrypt.sh`](file:///e:/auth/nginx/init-letsencrypt.sh):
   ```bash
   DOMAIN="api.lodhi.com"      # your real domain
   EMAIL="your@email.com"      # Let's Encrypt notifications
   ```
3. Run the bootstrap script:
   ```powershell
   wsl bash ./nginx/init-letsencrypt.sh
   ```
4. Restart nginx to pick up the real certs:
   ```powershell
   cd e:\auth\nginx
   wsl docker compose down
   wsl docker compose up -d
   ```

The certbot container automatically renews certs every 12 hours.

---

## Common Commands Cheat Sheet

| Action | Command |
|--------|---------|
| **Start everything** | `wsl docker network create authnet` (once) → start auth → start nginx |
| **Stop nginx** | `cd e:\auth\nginx && wsl docker compose down` |
| **Stop auth** | `cd e:\auth\auth_service && wsl docker compose down` |
| **Stop both** | Stop nginx first, then auth |
| **View nginx logs** | `wsl docker logs -f nginx-gateway` |
| **View auth logs** | `wsl docker logs -f auth-service` |
| **Reload nginx config** (no downtime) | `wsl docker exec nginx-gateway openresty -t && wsl docker exec nginx-gateway openresty -s reload` |
| **Rebuild nginx** | `cd e:\auth\nginx && wsl docker compose up -d --build` |
| **Check SSL cert** | `wsl curl -kvI https://localhost 2>&1 \| grep "subject"` |
| **Test rate limiting** | Send 5+ requests rapidly — you'll get HTTP 429 |
| **Renew certs manually** | `wsl docker compose run --rm certbot certbot renew --dry-run` |

---

## Startup Order

```
1. docker network create authnet     (once, persists across reboots)
2. auth-service compose up           (needs database to be reachable)
3. nginx compose up                  (needs auth-service on authnet)
```

Nginx will start even if auth-service is down — it will return a
`502 upstream_unavailable` JSON error until auth-service becomes healthy.

---

## Troubleshooting

### "upstream_unavailable" on every request
Auth-service isn't reachable. Check:
```powershell
wsl docker ps                           # is auth-service running?
wsl docker network inspect authnet      # are both containers on authnet?
wsl docker exec nginx-gateway ping -c1 auth-service   # can nginx reach it?
```

### "Connection refused" on port 443
Nginx didn't start. Check:
```powershell
wsl docker logs nginx-gateway           # look for cert generation errors
wsl docker exec nginx-gateway openresty -t   # config syntax check
```

### Self-signed cert warning in browser
Expected for local dev. Use `-k` with curl. In a browser, click
"Advanced" → "Proceed to localhost". For production, run
`init-letsencrypt.sh` to get real certs.

### CORS errors from your frontend
Edit [`lua/lib/cors.lua`](file:///e:/auth/nginx/lua/lib/cors.lua) — add your
frontend URL to `ALLOWED_ORIGINS`:
```lua
local ALLOWED_ORIGINS = {
    "https://yourdomain.com",
    "http://localhost:3000",   -- React/Vue dev server
}
```
Then reload: `wsl docker exec nginx-gateway openresty -s reload`

---

## Project File Structure

```
e:\auth\
├── auth_service/              ← Spring Boot auth service
│   ├── docker-compose.yml     ← joins external authnet network
│   ├── Dockerfile             ← multi-stage Maven → JRE Alpine
│   ├── .env                   ← local config
│   └── .env.prod              ← production config
│
└── nginx/                     ← OpenResty gateway
    ├── docker-compose.yml     ← nginx + certbot, external authnet
    ├── Dockerfile             ← OpenResty + auto self-signed certs
    ├── nginx.conf             ← main config (logging, lua path, gzip)
    ├── nginx.env.example      ← environment template
    ├── init-letsencrypt.sh    ← one-time Let's Encrypt bootstrap
    │
    ├── conf.d/
    │   ├── auth-service.conf  ← server blocks (HTTP redirect + HTTPS)
    │   ├── upstream.conf      ← backend definitions
    │   ├── rate-limit.conf    ← rate + connection limiting zones
    │   ├── proxy-common.conf  ← shared proxy headers
    │   └── security-headers.conf ← HSTS, X-Frame-Options, etc.
    │
    ├── lua/
    │   ├── validate_request.lua  ← main access_by_lua script
    │   └── lib/
    │       ├── json_response.lua ← JSON error response helper
    │       ├── ip_utils.lua      ← real client IP extraction
    │       └── cors.lua          ← CORS preflight + headers
    │
    └── ssl/
        ├── generate-self-signed.sh   ← cert generator script
        ├── ssl-params.conf           ← hardened TLS parameters
        └── options-ssl-nginx.conf    ← Mozilla Intermediate profile
```
