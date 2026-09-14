-- lua/lib/json_response.lua
--
-- Tiny helper to send a consistent JSON error response from any
-- access_by_lua / content_by_lua script.  Centralises the boilerplate
-- of setting status + Content-Type + body + exit, so individual
-- scripts never have to repeat it.
--
-- Usage:
--   local json_response = require("json_response")
--   json_response.send(403, "forbidden", "You do not have access")

local _M = {}

--- Send a JSON error response and terminate further processing.
-- @param status  HTTP status code (number)
-- @param error_code  Machine-readable error key, e.g. "missing_token"
-- @param message  Human-readable explanation
-- @param extra_headers  (optional) table of additional headers to set
function _M.send(status, error_code, message, extra_headers)
    ngx.status = status
    ngx.header["Content-Type"] = "application/json"

    if extra_headers then
        for k, v in pairs(extra_headers) do
            ngx.header[k] = v
        end
    end

    -- Use cjson if available (ships with OpenResty), fall back to
    -- manual string concat for absolute safety.
    local ok, cjson = pcall(require, "cjson.safe")
    if ok then
        ngx.say(cjson.encode({
            error   = error_code,
            message = message,
        }))
    else
        -- Manual JSON — only two fields, both strings, safe to escape
        -- with gsub.
        local safe_code = (error_code or ""):gsub('"', '\\"')
        local safe_msg  = (message or ""):gsub('"', '\\"')
        ngx.say('{"error":"' .. safe_code .. '","message":"' .. safe_msg .. '"}')
    end

    return ngx.exit(status)
end

--- Send a 429 "rate limited" response with a Retry-After header.
-- Convenience wrapper around send().
function _M.rate_limited(retry_after_seconds)
    retry_after_seconds = retry_after_seconds or 60
    return _M.send(429, "rate_limited",
        "Too many requests. Try again in " .. retry_after_seconds .. " seconds.",
        { ["Retry-After"] = tostring(retry_after_seconds) })
end

return _M
