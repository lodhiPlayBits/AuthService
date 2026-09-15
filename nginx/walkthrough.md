# Walkthrough — Nginx + LuaJIT Completion & SSL Files

## Summary

Completed the Nginx/OpenResty gateway by filling in the empty `lua/lib/` and `ssl/` directories, updating the Dockerfile to auto-generate self-signed certs on first boot, and hardening the TLS configuration.

## Changes Made

### New Lua Libraries (`lua/lib/`)

| File | Purpose |
|------|---------|
| [`json_response.lua`](file:///e:/auth/nginx/lua/lib/json_response.lua) | Centralised JSON error response helper — eliminates `ngx.status`/`ngx.say`/`ngx.exit` boilerplate. Uses OpenResty's bundled `cjson`. |
| [`ip_utils.lua`](file:///e:/auth/nginx/lua/lib/ip_utils.lua) | Extracts real client IP from `CF-Connecting-IP` → `X-Real-IP` → `X-Forwarded-For` → `remote_addr` priority chain. |
| [`cors.lua`](file:///e:/auth/nginx/lua/lib/cors.lua) | Handles CORS preflight (OPTIONS → 204) and sets `Access-Control-*` headers. Configurable allowed origins at the top of the file. |

### Refactored Lua Script

| File | Changes |
|------|---------|
| [`validate_request.lua`](file:///e:/auth/nginx/lua/validate_request.lua) | Now uses `json_response` and `cors` libs. Added `X-Request-ID` injection (generates via OpenResty's built-in `$request_id` if client doesn't send one). |

### New SSL Files (`ssl/`)

| File | Purpose |
|------|---------|
| [`generate-self-signed.sh`](file:///e:/auth/nginx/ssl/generate-self-signed.sh) | Generates self-signed cert + key + 2048-bit DH params. Mirrors Let's Encrypt path layout so config works with either cert source. |
| [`ssl-params.conf`](file:///e:/auth/nginx/ssl/ssl-params.conf) | Hardened TLS params — TLS 1.2+, modern ciphers, DH params, OCSP stapling, session tickets disabled for PFS. |
| [`options-ssl-nginx.conf`](file:///e:/auth/nginx/ssl/options-ssl-nginx.conf) | Mozilla "Intermediate" profile — drop-in alternative matching what certbot generates. |

### Modified Config Files

| File | Changes |
|------|---------|
| [`Dockerfile`](file:///e:/auth/nginx/Dockerfile) | Installs `openssl`, copies SSL configs, uses entrypoint script that auto-generates self-signed certs if no Let's Encrypt certs exist. |
| [`auth-service.conf`](file:///e:/auth/nginx/conf.d/auth-service.conf) | Includes `ssl-params.conf` for hardened TLS. Added section comments. |
| [`docker-compose.yml`](file:///e:/auth/nginx/docker-compose.yml) | Passes `DOMAIN` env var, adds healthcheck with `curl -k` (self-signed safe). |
| [`nginx.env.example`](file:///e:/auth/nginx/nginx.env.example) | Documents configurable variables (`DOMAIN`, `CERTBOT_EMAIL`, etc.). |

## Verification

- **Docker build**: ✅ All 12 steps passed, image `nginx-gateway-test:latest` built successfully.

## Before You Deploy

1. **Replace `yourdomain.com`** in [`auth-service.conf`](file:///e:/auth/nginx/conf.d/auth-service.conf) and [`init-letsencrypt.sh`](file:///e:/auth/nginx/init-letsencrypt.sh) with your real domain.
2. **Configure CORS origins** in [`cors.lua`](file:///e:/auth/nginx/lua/lib/cors.lua) — currently set to `"*"` (allow all). Lock this down to your frontend domain(s) in production.
3. **Run `init-letsencrypt.sh`** on first deploy to get real Let's Encrypt certs (the self-signed fallback is for local dev / first boot only).
