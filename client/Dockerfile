# Stage 1: Build the React App
FROM node:20-alpine AS builder
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build

# Stage 2: Serve via a lightweight internal Nginx
FROM nginx:alpine
# Copy the built React app from the builder stage
COPY --from=builder /app/dist /usr/share/nginx/html

# Overwrite the default Nginx config to support SPA routing (React Router)
RUN echo "server { listen 80; location / { root /usr/share/nginx/html; index index.html index.htm; try_files \$uri \$uri/ /index.html; } }" > /etc/nginx/conf.d/default.conf

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
