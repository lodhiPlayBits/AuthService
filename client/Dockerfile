# Stage 1: Build the React App
FROM node:20-alpine AS builder
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .

# Vite bakes VITE_* values into the bundle at build time.
# CI passes them as --build-arg. When an arg is not passed (local build),
# it is unset so Vite falls back to client/.env instead of an empty value.
ARG VITE_API_URL
ARG VITE_GOOGLE_CLIENT_ID
RUN [ -n "$VITE_API_URL" ] || unset VITE_API_URL; \
    [ -n "$VITE_GOOGLE_CLIENT_ID" ] || unset VITE_GOOGLE_CLIENT_ID; \
    npm run build

# Stage 2: Serve via a lightweight internal Nginx
FROM nginx:alpine
# Copy the built React app from the builder stage
COPY --from=builder /app/dist /usr/share/nginx/html

# Overwrite the default Nginx config to support SPA routing (React Router)
RUN echo "server { listen 80; location / { root /usr/share/nginx/html; index index.html index.htm; try_files \$uri \$uri/ /index.html; } }" > /etc/nginx/conf.d/default.conf

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]