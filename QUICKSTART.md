# 🚀 Quick Start Guide: Nginx + Auth Service

Don't worry! You **do not** need to manually generate any certificates right now, and you **do not** need a domain name to test this. Nginx is configured to do the heavy lifting for you automatically.

Here is the exact step-by-step process to start both services together and test them.

---

## Step 1: Create the Shared Network
Both Nginx and your Auth Service need to talk to each other. We use a Docker network for this.
Run this command **once**:

```powershell
wsl docker network create authnet
```

---

## Step 2: Start the Auth Service
First, we start your Spring Boot application so Nginx has something to connect to.

1. Open your terminal and navigate to the `auth_service` folder:
   ```powershell
   cd e:\auth\auth_service
   ```
2. Start the service (make sure your `.env` file is ready):
   ```powershell
   wsl docker compose --env-file .env up -d --build
   ```

---

## Step 3: Start the Nginx Reverse Proxy
Now we start Nginx. 

**Note on Certificates:** When Nginx boots up, it will notice you don't have real certificates yet. It will *automatically* generate a temporary "self-signed" certificate in the background so it can run securely on HTTPS (Port 443). You don't have to run any scripts yourself!

1. Navigate to the `nginx` folder:
   ```powershell
   cd e:\auth\nginx
   ```
2. Start Nginx:
   ```powershell
   wsl docker compose up -d --build
   ```

---

## Step 4: How to Test in Your Browser

Because Nginx automatically generated a "self-signed" (temporary) certificate, your browser won't recognize the issuer and will throw a security warning. **This is completely normal and expected.**

1. Open your web browser (Chrome, Edge, Firefox, etc.).
2. In the URL bar, type:
   - If testing on your local PC: `https://localhost/health`
   - If testing on AWS EC2: `https://<YOUR-EC2-PUBLIC-IP>/health`
3. You will see a big warning screen saying **"Your connection is not private"** or **"Potential Security Risk Ahead"**.
4. **How to bypass it:**
   - **Chrome/Edge:** Click the `Advanced` button at the bottom, then click `Proceed to localhost (unsafe)` or `Proceed to <IP> (unsafe)`.
   - **Firefox:** Click `Advanced`, then `Accept the Risk and Continue`.
5. You should now see the health response from your Auth Service! It means Nginx successfully caught your request on port 443 and routed it to your Auth Service on port 8080!

---

## Step 5: How to Test with Postman or cURL

If you are using tools like Postman to test your login/register APIs, they will also complain about the self-signed certificate.

- **In Postman:** Go to Settings (the gear icon) > General > Turn **OFF** "SSL certificate verification". Now you can make `POST` requests to `https://localhost/api/auth/login` without errors.
- **Using cURL in terminal:** Add the `-k` (or `--insecure`) flag to tell cURL to trust the temporary certificate:
  ```powershell
  wsl curl -k https://localhost/health
  ```

---

## Summary
That's it! You have successfully deployed a reverse proxy. 
- You didn't need to manually create certs. 
- You didn't need a domain.
- Nginx is protecting your Spring Boot app, handling CORS, rate-limiting, and managing the HTTPS encryption. 

When you eventually buy a domain for production, you will just run the `init-letsencrypt.sh` script to replace this temporary setup with real, trusted certificates (as explained in the `nginx/README.md`).
