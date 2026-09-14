-- lua/lib/ip_utils.lua
--
-- Extracts the "real" client IP from the request, accounting for
-- reverse proxies and CDNs sitting in front of this gateway.
--
-- Priority order:
--   1. CF-Connecting-IP  (Cloudflare — most trustworthy when present)
--   2. X-Real-IP         (set by an outer proxy you control)
--   3. First entry in X-Forwarded-For  (leftmost = original client)
--   4. ngx.var.remote_addr  (direct connection, always available)
--
-- IMPORTANT: headers 1–3 are only trustworthy if you've configured
-- `set_real_ip_from` in nginx.conf to restrict which source IPs are
-- allowed to set them.  Without that, any client can spoof these
-- headers and bypass per-IP rate limiting.  See nginx.conf for the
-- commented-out set_real_ip_from block.

local _M = {}

--- Return the best-guess real client IP as a string.
function _M.get_real_ip()
    local headers = ngx.req.get_headers()

    -- 1. Cloudflare
    local cf_ip = headers["cf-connecting-ip"]
    if cf_ip and cf_ip ~= "" then
        return cf_ip
    end

    -- 2. Explicit X-Real-IP set by a trusted outer proxy
    local real_ip = headers["x-real-ip"]
    if real_ip and real_ip ~= "" then
        return real_ip
    end

    -- 3. X-Forwarded-For — take the leftmost (original client) entry.
    --    The header value can be a comma-separated list:
    --    "client, proxy1, proxy2"
    local xff = headers["x-forwarded-for"]
    if xff and xff ~= "" then
        local first = xff:match("^%s*([^,%s]+)")
        if first then
            return first
        end
    end

    -- 4. Direct connection
    return ngx.var.remote_addr
end

return _M
