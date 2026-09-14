# AWS EC2 Deployment Guide

## Prerequisites
- AWS EC2 instance running (Amazon Linux 2 or Ubuntu)
- SSH access to EC2
- Your repo pushed to GitHub (or files available to SCP)

---

## Step 1: Install Docker on EC2

SSH into your EC2 instance:
```bash
ssh -i your-key.pem ec2-user@<EC2_PUBLIC_IP>
```

### Amazon Linux 2:
```bash
sudo yum update -y
sudo yum install -y docker git
sudo systemctl start docker
sudo systemctl enable docker
sudo usermod -aG docker ec2-user

# Install Docker Compose plugin
sudo mkdir -p /usr/local/lib/docker/cli-plugins
sudo curl -SL "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" \
  -o /usr/local/lib/docker/cli-plugins/docker-compose
sudo chmod +x /usr/local/lib/docker/cli-plugins/docker-compose

# IMPORTANT: Log out and back in for group changes
exit
ssh -i your-key.pem ec2-user@<EC2_PUBLIC_IP>

# Verify
docker --version
docker compose version
```

### Ubuntu:
```bash
sudo apt update && sudo apt install -y docker.io git
sudo systemctl start docker
sudo systemctl enable docker
sudo usermod -aG docker ubuntu

# Install Docker Compose plugin
sudo mkdir -p /usr/local/lib/docker/cli-plugins
sudo curl -SL "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" \
  -o /usr/local/lib/docker/cli-plugins/docker-compose
sudo chmod +x /usr/local/lib/docker/cli-plugins/docker-compose

exit
ssh -i your-key.pem ubuntu@<EC2_PUBLIC_IP>
```

---

## Step 2: Get your code onto EC2

### Option A: Git clone
```bash
git clone https://github.com/YOUR_USERNAME/auth.git
cd auth
```

### Option B: SCP from your laptop
```bash
# Run this FROM your laptop (PowerShell/WSL)
scp -i your-key.pem -r /mnt/e/auth ec2-user@<EC2_PUBLIC_IP>:~/auth
```

---

## Step 3: Configure .env.prod on EC2

```bash
cd ~/auth
nano .env.prod
```

Fill in the two critical values:

```properties
# Your laptop's reachable IP or tunnel address
DB_HOST=<YOUR_LAPTOP_IP>

# Generate a fresh secret
JWT_SECRET=<paste output of: openssl rand -base64 64>
```

Generate the JWT secret right on EC2:
```bash
openssl rand -base64 64
```

---

## Step 4: Prepare your laptop's PostgreSQL

### 4a. Allow remote connections in PostgreSQL

In WSL, edit PostgreSQL config:
```bash
# Find your PostgreSQL version
ls /etc/postgresql/

# Edit postgresql.conf
sudo nano /etc/postgresql/<version>/main/postgresql.conf
# Change:  listen_addresses = '*'

# Edit pg_hba.conf — add this line at the end:
sudo nano /etc/postgresql/<version>/main/pg_hba.conf
# Add:  host  all  all  0.0.0.0/0  md5

# Restart PostgreSQL
sudo systemctl restart postgresql
```

### 4b. Open Windows Firewall

Open PowerShell as Administrator:
```powershell
New-NetFirewallRule -DisplayName "PostgreSQL" -Direction Inbound -Protocol TCP -LocalPort 5432 -Action Allow
```

### 4c. Port forward on your router

Forward external port 5432 → your laptop's local IP:5432.

Find your local IP:
```powershell
ipconfig | findstr "IPv4"
```

Find your public IP:
```powershell
curl ifconfig.me
```

Set `DB_HOST` in `.env.prod` to your **public IP**.

---

## Step 5: EC2 Security Group

In AWS Console → EC2 → Security Groups → Edit inbound rules:

| Type       | Port  | Source        | Purpose          |
|------------|-------|---------------|------------------|
| SSH        | 22    | My IP         | SSH access       |
| Custom TCP | 8080  | 0.0.0.0/0     | Auth service API |

---

## Step 6: Deploy

```bash
cd ~/auth
docker compose --env-file .env.prod up -d --build
```

First build takes ~3-5 minutes (downloading Maven dependencies).

### Check status:
```bash
docker compose ps
docker compose logs -f auth-service
```

### Verify health:
```bash
# From EC2
curl http://localhost:8080/actuator/health

# From your laptop browser
curl http://<EC2_PUBLIC_IP>:8080/actuator/health
```

---

## Useful Commands

```bash
# View logs
docker compose --env-file .env.prod logs -f

# Restart
docker compose --env-file .env.prod restart

# Stop
docker compose --env-file .env.prod down

# Rebuild and restart
docker compose --env-file .env.prod up -d --build

# Check if container can reach your laptop's DB
docker compose exec auth-service sh -c "nc -zv <YOUR_LAPTOP_IP> 5432"
```

---

## Troubleshooting

| Problem | Fix |
|---------|-----|
| `Connection refused` to DB | Check PostgreSQL is listening (`listen_addresses = '*'`), pg_hba.conf allows the IP, Windows Firewall is open, router port forwarding is set |
| `No route to host` | Your public IP may have changed. Check `curl ifconfig.me` and update `.env.prod` |
| Container starts but health check fails | Check `docker compose logs` for the actual Spring Boot error |
| Build fails on EC2 | Ensure EC2 has at least 2GB RAM (t2.small or larger). t2.micro may OOM during Maven build |
