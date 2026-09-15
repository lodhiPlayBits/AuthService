-- lua/lib/cors.lua
--
-- Centralised CORS handling for the API gateway.
--
-- Call cors.handle() in access_by_lua_* — it will:
--   • Respond to preflight OPTIONS requests immediately (204, no body)
--   • Set Access-Control-Allow-* headers on every response
--
-- Configuration is at the top of this file.  Edit ALLOWED_ORIGINS,
-- ALLOWED_METHODS, etc. to match your frontend deployment.
--
-- Usage in a server block / location:
--   access_by_lua_block {
--       require("lib.cors").handle()
--       -- … other access checks …
--   }

local _M = {}

-- ─── Configuration ───────────────────────────────────────────────
-- Add every origin that should be allowed to call this API.
-- Use "*" (as the sole entry) to allow any origin — only suitable
-- for fully public APIs.
local ALLOWED_ORIGINS = {
    "https://authvolt.fun",
    "https://www.authvolt.fun",
    "http://localhost:5173",    -- local Vite dev server
    -- "*",  -- allow all origins (DO NOT use in production!)
}

local ALLOWED_METHODS  = "GET, POST, PUT, PATCH, DELETE, OPTIONS"
local ALLOWED_HEADERS  = "Authorization, Content-Type, X-Request-ID, Accept"
local EXPOSED_HEADERS  = "X-Request-ID"
local MAX_AGE          = "7200"   -- seconds browsers may cache the preflight
local ALLOW_CREDENTIALS = "true"  -- set to "false" if you don't use cookies / auth headers
-- ─────────────────────────────────────────────────────────────────

--- Check whether the request Origin is in ALLOWED_ORIGINS.
-- Returns the origin string to echo back, or nil.
local function match_origin(request_origin)
    if not request_origin or request_origin == "" then
        return nil
    end
    for _, allowed in ipairs(ALLOWED_ORIGINS) do
        if allowed == "*" then
            -- When credentials are enabled, the spec forbids echoing "*"
            -- so we echo back the actual origin instead.
            return request_origin
        end
        if allowed == request_origin then
            return request_origin
        end
    end
    return nil
end

--- Set CORS response headers if the Origin matches.
-- Call this from access_by_lua_*.  For OPTIONS (preflight) it sends
-- a 204 and stops processing.  For all other methods it just sets
-- the headers and lets the request continue to proxy_pass.
function _M.handle()
    local origin = ngx.req.get_headers()["origin"]
    local matched = match_origin(origin)

    if not matched then
        -- Origin not allowed (or no Origin header, i.e. same-origin /
        -- non-browser request) — nothing to add, let it through.
        return
    end

    -- Set CORS headers that apply to BOTH preflight and actual requests
    ngx.header["Access-Control-Allow-Origin"]   = matched
    ngx.header["Access-Control-Allow-Credentials"] = ALLOW_CREDENTIALS
    ngx.header["Access-Control-Expose-Headers"] = EXPOSED_HEADERS
    ngx.header["Vary"] = "Origin"

    -- Preflight (OPTIONS) — respond immediately, no need to hit the backend
    if ngx.req.get_method() == "OPTIONS" then
        ngx.header["Access-Control-Allow-Methods"] = ALLOWED_METHODS
        ngx.header["Access-Control-Allow-Headers"] = ALLOWED_HEADERS
        ngx.header["Access-Control-Max-Age"]       = MAX_AGE
        ngx.header["Content-Length"] = "0"
        return ngx.exit(204)
    end

    -- Non-preflight: headers are set, let the request continue
end

return _M
