-- Gateway-level request validation.
--
-- IMPORTANT: this intentionally does NOT verify JWT signatures. That
-- stays the sole responsibility of Spring Security in auth-service —
-- having two independent authorities decide "is this token valid"
-- is a bug waiting to happen, not a feature. This script only rejects
-- obviously-bad requests cheaply, before they consume a backend
-- worker thread. Think of it as a bouncer checking you have a ticket-
-- shaped object in your hand, not checking if the ticket is real.

local json_response = require("lib.json_response")
local cors          = require("lib.cors")

-- ── 0. CORS ──────────────────────────────────────────────────────
-- Handle preflight OPTIONS and set Access-Control-* headers.
-- If this is an OPTIONS request, cors.handle() sends 204 and stops
-- processing — nothing below runs.
cors.handle()

-- ── 1. Inject X-Request-ID ──────────────────────────────────────
-- If the client didn't send one, generate a unique ID for end-to-end
-- request tracing through the gateway → auth-service → logs.
local request_id = ngx.req.get_headers()["x-request-id"]
if not request_id or request_id == "" then
    -- ngx.var.request_id is a built-in OpenResty variable that gives
    -- a 32-hex-char unique ID per request — no external dependency.
    request_id = ngx.var.request_id
end
ngx.req.set_header("X-Request-ID", request_id)
-- Also put it on the response so the client can quote it in bug reports
ngx.header["X-Request-ID"] = request_id

-- ── 2. Reject missing Content-Type on state-changing methods ────
local method = ngx.req.get_method()
if method == "POST" or method == "PUT" or method == "PATCH" then
    local content_type = ngx.req.get_headers()["content-type"]
    if not content_type or not string.find(content_type, "application/json") then
        return json_response.send(415, "unsupported_media_type",
            "Content-Type must be application/json")
    end
end

-- ── 3. Require Authorization: Bearer <token> on protected paths ─
-- This is a cheap presence/format check only — Spring Security
-- still does the real signature + claims verification.
local uri = ngx.var.uri
local is_public = string.find(uri, "^/api/auth/login")
                  or string.find(uri, "^/api/auth/register")
                  or string.find(uri, "^/api/auth/refresh")

if not is_public then
    local auth_header = ngx.req.get_headers()["authorization"]
    if not auth_header or not string.find(auth_header, "^Bearer%s+%S+") then
        return json_response.send(401, "missing_token",
            "Authorization: Bearer <token> header is required")
    end
end

-- ── 4. Body size sanity check ───────────────────────────────────
-- Already handled by client_max_body_size in nginx.conf — no need
-- to duplicate here.

-- Passed all cheap checks. Request proceeds to proxy_pass, where
-- Spring Security in auth-service does the real authentication.
