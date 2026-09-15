# ── Client Service ───────────────────────────────────────────────
# Proxy /client requests to the React client container.
# This is a separate service from auth-service, running its own
# Docker container on the shared authnet network.
#
# This file is included inside the HTTPS server block in auth-service.conf
# via:  include /usr/local/openresty/nginx/conf/conf.d/client-service.inc;

# Redirect /client to /client/ (trailing slash needed for proxy_pass stripping)
location = /client {
    return 301 /client/;
}

# Strip /client/ prefix and forward to the client container's root
location /client/ {
    proxy_pass http://client_backend/;
    include /usr/local/openresty/nginx/conf/conf.d/proxy-common.conf;
}
