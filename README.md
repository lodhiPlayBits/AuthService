# AuthService · AuthVolt

[![CI](https://github.com/lodhiPlayBits/AuthService/actions/workflows/ecr-push.yml/badge.svg?branch=main)](https://github.com/lodhiPlayBits/AuthService/actions/workflows/ecr-push.yml)
[![Release](https://img.shields.io/github/v/release/lodhiPlayBits/AuthService)](https://github.com/lodhiPlayBits/AuthService/releases/latest)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

**Production-oriented authentication, authorization and real-time notification platform built with Spring Boot 4, PostgreSQL, Redis, Kafka and an OpenResty/Nginx gateway.**

AuthVolt provides secure user authentication: JWT access tokens, refresh-token rotation with token-family revocation, Google sign-in, role-based access control with fine-grained permissions, and gateway-level request protection. A separate notification service streams account events and admin announcements to users in real time over Kafka, Server-Sent Events and STOMP/WebSocket.

The system is built backend-first, with security, database safety, observability, rate limiting and automated production deployment in mind.

**Live:** [authvolt.fun](https://authvolt.fun) · **Changelog:** [CHANGELOG.md](CHANGELOG.md)

---

## Contents

- [Features](#features)
- [Architecture](#architecture): runtime, CI/CD, design decisions, gateway, services, Kafka, data model
- [Authentication flows](#authentication-flows): login, authorization, refresh, logout, Google
- [Real-time notifications](#real-time-notifications): SSE, account events, WebSocket, announcements
- [Tech stack](#tech-stack) · [Project structure](#project-structure) · [API endpoints](#api-endpoints)
- [Environment](#environment-configuration) · [Running locally](#running-locally) · [Testing](#testing)
- [Deployment](#deployment) · [Security](#security-considerations) · [Roadmap](#future-improvements)

---

## Features

### Authentication

* JWT access tokens (HS512, issuer and audience validated)
* Short-lived access tokens, long-lived refresh tokens in `HttpOnly` cookies scoped to `/api/v1/auth`
* Refresh-token rotation with an atomic "revoke if still active" update
* Refresh-token reuse detection and token-family revocation
* Refresh tokens stored as SHA-256 hashes, never in plain text
* CSRF double-submit protection on cookie endpoints (`XSRF-TOKEN` cookie + `X-XSRF-TOKEN` header)
* Login with **email, username or phone number**
* Google sign-in with a profile-completion step and no silent account linking
* Logout and logout-all-devices, with the current access token blacklisted in Redis
* BCrypt on virtual threads behind a semaphore, so login bursts can't starve the CPU

### Authorization / RBAC

* Roles and fine-grained permissions stored in the database and editable at runtime
* Roles and permissions carried in the access token, so there's no database lookup per request
* `@PreAuthorize` with **ownership rules** on user endpoints
* Custom `@RequiresPermission` aspect on admin endpoints
* Assign or revoke permissions on roles, assign roles to users, enable or disable accounts
* Audit log of every security-relevant event

### Real-time notifications

* Account events published to **Kafka**, keyed by user ID so each user's events stay in order
* Pushed to every open browser tab over **Server-Sent Events**
* Every event is also kept in **Redis** (last 50, 7 days) and replayed when a client reconnects with `Last-Event-ID`
* Revoked, disabled or deleted users have their SSE streams and WebSocket sessions closed by the server
* Admin announcements over **STOMP/WebSocket**, fanned out to every service instance through Kafka, with ack and dismiss
* JWT-authenticated WebSocket handshake, admin-only topics, and expired sessions closed every minute
* Retries and dead-letter topics for events that keep failing

### Gateway security

* TLS termination with HTTP → HTTPS redirect, Let's Encrypt certificates renewed by `certbot`
* Security headers: HSTS, `X-Frame-Options`, `X-Content-Type-Options`, `Referrer-Policy`, `Permissions-Policy`
* Per-route rate limiting: 3 r/s for login, register and refresh, and 10 r/s for everything else
* 20 connections per IP
* Lua request validation (CORS, request ID, `Content-Type`, `Bearer` presence) before requests reach the JVM
* Self-signed certificate fallback for local development

### Observability

* `X-Request-ID` added at the gateway, written to every log line, and carried through Kafka headers into notification-service
* Structured request logging, Actuator health probes, and an audit log table
* Kafka UI and a Prometheus Kafka exporter

### CI/CD & DevSecOps

* Tests, JaCoCo, Trivy filesystem and image scans, and SonarCloud on every PR
* Only changed services are rebuilt. Images are tagged by commit SHA in Amazon ECR.
* Deploy on merge to `main` via AWS SSM, with health checks, two layers of smoke tests and **automatic rollback**

---

# Architecture

## Runtime

```mermaid
flowchart LR
    user(["Browser"])
    google(["Google OAuth"])

    subgraph aws["AWS ap-northeast-1"]
        direction LR
        subgraph ec2["EC2 t3.large · Docker Compose"]
            direction LR
            nginx["nginx-gateway<br/>OpenResty · TLS<br/>rate limiting · Lua validation"]
            client["client-service<br/>React SPA"]
            auth["auth-service<br/>Spring Boot · :5001"]
            notif["notification-service<br/>Spring Boot · :5002"]
            redis[("Redis")]
            subgraph kafka["Kafka · KRaft"]
                k1[("broker 1")]
                k2[("broker 2")]
            end
            certbot["certbot<br/>Let's Encrypt"]
        end
        rds[("PostgreSQL<br/>Amazon RDS")]
    end

    user -->|"HTTPS authvolt.fun"| nginx
    user -.->|"Sign in with Google"| google
    nginx -->|"/client/"| client
    nginx -->|"/api/v1/* · /health"| auth
    nginx -->|"/api/v1/notifications · /ws"| notif
    auth -->|"JPA · Flyway"| rds
    auth -->|"roles cache · token blacklist"| redis
    auth -->|"verify ID token"| google
    auth -->|"account-events"| kafka
    kafka -->|"consume events"| notif
    notif -->|"missed-event buffer"| redis
    notif -.->|"SSE · WebSocket"| nginx
    certbot -.->|"renews certificates"| nginx
```

## CI/CD

```mermaid
flowchart LR
    pr(["Pull request<br/>or merge"])

    subgraph ci["GitHub Actions · every PR and merge"]
        direction TB
        test["test<br/>Maven + JaCoCo · Vitest + ESLint"]
        scan["scan<br/>Trivy dependencies<br/>fails on fixable CRITICAL"]
        sonar["sonar<br/>SonarCloud analysis"]
        build["build-push<br/>changed service → build + Trivy image gate<br/>unchanged service → reuse previous image"]
        passed{{"ci-passed<br/>required check"}}
        test --> build
        scan --> build
        build --> passed
        sonar --> passed
    end

    subgraph cd["Merge to main only"]
        direction TB
        deploy["deploy<br/>SSM → deploy.sh<br/>health checks + smoke tests on VM<br/>auto-rollback to last good tag"]
        smoke["smoke-test<br/>public endpoints via DNS + TLS"]
        rollback["rollback<br/>redeploy previous version<br/>open revert PR"]
        deploy --> smoke
        smoke -.->|"on failure"| rollback
    end

    ecr[("Amazon ECR<br/>6 repositories")]
    vm["EC2"]

    pr --> ci
    passed --> deploy
    build -->|"push via OIDC"| ecr
    deploy -->|"aws ssm send-command"| vm
    ecr -->|"pull · instance role"| vm
```

## Design decisions

| Decision | Why |
|---|---|
| GitHub OIDC → IAM role | No long-lived AWS keys stored in GitHub; trust limited to `main` and `develop` |
| AWS SSM instead of SSH | No open port 22, no SSH keys in CI |
| Reuse unchanged images | App-only changes don't rebuild or restart Kafka and Redis |
| Health checks + smoke tests before marking a deploy good | A running container isn't proof the service works; failure rolls back automatically |
| Revert PR instead of auto-revert | Branch protection stays intact; a human decides whether the code was at fault |
| Docker Compose instead of Kubernetes | Single node: Kubernetes adds cost and complexity with no benefit at this scale |
| Kafka between auth and notification services | auth-service never waits on notification delivery, and events survive a notification-service restart |
| Account status events sent after the DB commit | A rolled-back enable or disable never logs a user out |
| Atomic "revoke if active" update on refresh | Two concurrent refreshes with the same token can't both succeed; the loser is treated as reuse |
| One Kafka consumer group per instance for announcements | Every instance gets every announcement, so users connected to any instance receive it |
| HS512 shared secret for JWTs | Both services verify tokens locally with no call to auth-service; asymmetric keys are on the roadmap |

## Gateway request routing

```mermaid
flowchart TB
    browser(["Browser"])

    subgraph p80["server :80"]
        direction LR
        acme["/.well-known/acme-challenge/<br/>served from certbot webroot"]
        redirect["everything else<br/>301 → https"]
    end

    subgraph p443["server :443 · authvolt.fun"]
        direction TB
        tls["TLS termination<br/>Let's Encrypt certificate"]
        headers["Security headers<br/>HSTS · X-Frame-Options · nosniff<br/>Referrer-Policy · Permissions-Policy"]
        conn["limit_conn · 20 per IP"]
        rootloc["location = /<br/>301 → /client/"]
        clientloc["location /client/"]
        loginloc["location ~ /api/v1/auth/ login · register · refresh<br/>login_limit 3 r/s · burst 5"]
        notifloc["location ^~ /api/v1/notifications/<br/>api_limit 10 r/s · burst 20<br/>buffering off · 24 h read timeout"]
        wsloc["location ^~ /ws/<br/>api_limit · Upgrade: websocket<br/>1 h read timeout"]
        apiloc["location /api/v1/<br/>api_limit 10 r/s · burst 20"]
        healthloc["location /health"]
        lua["Lua validate_request<br/>1 · CORS<br/>2 · add X-Request-ID<br/>3 · missing Content-Type → 415<br/>4 · no Bearer on protected path → 401"]
        tls --> headers --> conn
        conn --> rootloc & clientloc & loginloc & notifloc & wsloc & apiloc & healthloc
        loginloc --> lua
        apiloc --> lua
    end

    client["client-service :80"]
    auth["auth-service :5001"]
    notif["notification-service :5002"]

    browser -->|"HTTP"| p80
    browser -->|"HTTPS"| tls
    clientloc --> client
    lua --> auth
    healthloc -->|"/actuator/health"| auth
    notifloc -->|"SSE"| notif
    wsloc -->|"STOMP"| notif
```

Request bodies are capped at 10 MB by `client_max_body_size`.

## auth-service internals

```mermaid
flowchart TB
    req(["Request from nginx"])

    subgraph filters["1 · Filter chain"]
        direction LR
        lf["LoggingFilter<br/>X-Request-ID → MDC · request log"]
        csrf["CsrfFilter + CsrfCookieFilter<br/>enforced on /refresh, /logout, /logout/all"]
        jf["JwtAuthenticationFilter<br/>HS512 · iss · aud · exp · type = access<br/>Redis blacklist check"]
        lf --> csrf --> jf
    end

    subgraph web["2 · Controllers"]
        direction LR
        ac["AuthController<br/>/api/v1/auth"]
        oc["OAuth2Controller<br/>/api/v1/auth/oauth2"]
        uc["UserController<br/>/api/v1/users<br/>@PreAuthorize + ownership"]
        adc["AdminController<br/>/api/v1/admin<br/>@RequiresPermission aspect"]
    end

    subgraph svc["3 · Services"]
        direction LR
        subgraph tokensvc["Sessions and tokens"]
            direction TB
            as["AuthService<br/>login · logout · logout-all"]
            rts["RefreshTokenService<br/>rotation · reuse detection"]
            rfs["RefreshTokenFamilyService<br/>revoke family"]
            tbs["TokenBlacklistService<br/>jti with TTL"]
        end
        subgraph idsvc["Identity and RBAC"]
            direction TB
            gs["GoogleOAuth2Service<br/>ID-token sign-in"]
            us["UserService · AdminService<br/>RoleService · PermissionService"]
        end
        subgraph shared["Shared"]
            direction TB
            bs["BCryptService<br/>virtual threads · 6 permits · 30 s wait"]
            aep["AccountEventProducer<br/>key = userId · X-Request-ID header"]
            aud["AuditService<br/>security audit log"]
        end
    end

    subgraph sec["4 · Security helpers"]
        direction LR
        cap["CustomAuthenticationProvider<br/>email · username · phone"]
        js["JwtService<br/>sign + parse HS512"]
        th["TokenHashService<br/>SHA-256 of jti"]
        cs["CookieService<br/>HttpOnly · Secure · SameSite"]
    end

    subgraph repo["5 · Repositories · Spring Data JPA"]
        direction LR
        r1["User · Role · Permission"]
        r2["RefreshToken"]
        r3["AuditLog"]
    end

    pg[("PostgreSQL · RDS")]
    redis[("Redis")]
    kafka[["Kafka · account-events"]]
    google(["Google"])

    req --> filters
    filters --> web
    web --> svc
    svc --> sec
    svc --> repo
    repo --> pg
    tbs -->|"blacklist"| redis
    us -->|"roles cache"| redis
    aep -->|"publish"| kafka
    gs -->|"verify ID token"| google
```

## notification-service internals

### SSE side

```mermaid
flowchart LR
    browser(["Browser<br/>EventSource"])
    ae[["Kafka<br/>account-events"]]
    redis[("Redis<br/>notification:v1:sse:missed:userId")]

    subgraph ns["notification-service · Server-Sent Events"]
        direction TB
        sec["Resource server<br/>HS512 · iss · aud<br/>Bearer header or ?token="]
        nc["NotificationController<br/>GET /api/v1/notifications/stream"]
        scm["SseConnectionManager<br/>5 streams per user · 1000 total<br/>1 h emitter timeout"]
        hb["SseHeartbeatScheduler<br/>ping every 30 s"]
        aec["AccountEventConsumer<br/>group notification-group"]
        wsm["WebSocketSessionManager"]
    end

    browser -->|"connect · Last-Event-ID"| sec
    sec --> nc
    nc --> scm
    nc -->|"replay missed events"| redis
    hb --> scm
    ae --> aec
    aec -->|"push to every open stream"| scm
    aec -->|"RPUSH · LTRIM 50 · EXPIRE 7 d"| redis
    aec -->|"revoked · disabled · deleted<br/>close SSE + WebSocket after 1 s"| wsm
    scm -.->|"connected · events · ping"| browser
```

### WebSocket side

```mermaid
flowchart LR
    browser(["Browser<br/>STOMP client"])

    subgraph ns["notification-service · STOMP over WebSocket"]
        direction TB
        hs["JwtHandshakeInterceptor<br/>Origin allow-list · access token only"]
        hh["StompHandshakeHandler<br/>StompPrincipal: userId, roles, expiresAt"]
        wsm["WebSocketSessionManager<br/>expired-token sweep every 60 s"]
        sai["SubscriptionAuthorizationInterceptor<br/>/topic/admin.* needs ADMIN<br/>SEND only to /app/**"]
        stc["AnnouncementStompController<br/>send · ack · dismiss"]
        ap["AnnouncementProducer<br/>acks = all · idempotent"]
        anc["AnnouncementConsumer<br/>own group per instance · 24 h dedup"]
        broker["Simple broker<br/>/topic · /queue · heartbeat 10 s"]
    end

    subgraph k["Kafka"]
        direction TB
        ar[["admin-responses"]]
        ba[["broadcast-announcements"]]
        dlt[["broadcast-announcements.DLT"]]
    end

    browser -->|"/ws/announcements?token="| hs
    hs --> hh
    hh --> wsm
    browser -->|"SUBSCRIBE · SEND"| sai
    sai -->|"allowed subscriptions"| broker
    sai --> stc
    stc -->|"ack · dismiss"| ar
    stc -->|"send"| ap
    ap --> ba
    stc -->|"confirmations · errors"| broker
    ba --> anc
    ar --> anc
    anc -.->|"after 3 retries"| dlt
    anc --> broker
    broker -.->|"/topic/announcements<br/>/topic/admin.responses<br/>/user/queue/*"| browser
    wsm -.->|"close expired sessions"| browser
```

## Kafka topology

```mermaid
flowchart LR
    subgraph producers["Producers"]
        direction TB
        p1["auth-service<br/>AccountEventProducer"]
        p2["notification-service<br/>AnnouncementProducer"]
        p3["notification-service<br/>AnnouncementStompController"]
    end

    subgraph cluster["Kafka · KRaft · 2 brokers · replication factor 2"]
        direction TB
        ae[["account-events<br/>3 partitions · 7 days<br/>key = userId"]]
        ba[["broadcast-announcements<br/>3 partitions · 7 days<br/>key = announcementId"]]
        ar[["admin-responses<br/>3 partitions · 7 days<br/>key = announcementId"]]
    end

    subgraph consumers["Consumer groups"]
        direction TB
        g1["notification-group<br/>AccountEventConsumer<br/>shared: one instance handles each event"]
        g2["ws-announcement-uuid<br/>one group per instance<br/>every instance gets every announcement"]
        g3["ws-admin-responses-uuid<br/>one group per instance"]
    end

    subgraph dlts["Dead-letter topics"]
        direction TB
        aedlt[["account-events.DLT"]]
        badlt[["broadcast-announcements.DLT"]]
    end

    p1 -->|"ACCOUNT_DISABLED · ACCOUNT_ENABLED<br/>SESSION_REVOKED · header X-Request-ID"| ae
    p2 --> ba
    p3 -->|"USER_RESPONSE"| ar
    ae --> g1
    ba --> g2
    ar --> g3
    g1 -.->|"failed after retries"| aedlt
    g2 -.->|"3 retries · 1 s apart"| badlt
```

The `AccountEvent` contract also defines `ACCOUNT_DELETED`, `PASSWORD_CHANGED` and `ROLE_CHANGED`. notification-service already handles them, but auth-service doesn't publish them yet.

## Data model

```mermaid
erDiagram
    USER_TABLE ||--o{ USER_ROLES : "has"
    ROLE ||--o{ USER_ROLES : "assigned through"
    ROLE ||--o{ ROLE_PERMISSIONS : "grants"
    PERMISSIONS ||--o{ ROLE_PERMISSIONS : "granted through"
    USER_TABLE ||--o{ REFRESH_TOKENS : "owns"
    USER_TABLE |o--o{ AUDIT_LOGS : "subject of"

    USER_TABLE {
        BIGINT user_id PK
        VARCHAR email UK
        VARCHAR username UK
        VARCHAR phone_number UK "nullable for Google users"
        VARCHAR password "BCrypt, null for Google users"
        VARCHAR name
        VARCHAR gender
        VARCHAR image
        VARCHAR provider "LOCAL or GOOGLE"
        VARCHAR provider_id "Google subject, unique with provider"
        BOOLEAN is_enable
        BOOLEAN profile_complete
        TIMESTAMPTZ created_at
        TIMESTAMPTZ updated_at
    }
    ROLE {
        UUID id PK
        VARCHAR name UK
        VARCHAR description
        BOOLEAN is_system_role
    }
    PERMISSIONS {
        UUID id PK
        VARCHAR name UK "e.g. users:read"
        VARCHAR description
    }
    USER_ROLES {
        BIGINT user_id PK, FK
        UUID role_id PK, FK
    }
    ROLE_PERMISSIONS {
        UUID role_id PK, FK
        UUID permission_id PK, FK
    }
    REFRESH_TOKENS {
        UUID id PK
        VARCHAR jti_hash UK "SHA-256 of the token id"
        BIGINT user_id FK
        VARCHAR family_id "one login session"
        TIMESTAMPTZ created_at
        TIMESTAMPTZ expires_at
        BOOLEAN revoked
        VARCHAR replace_token "id of the token that replaced it"
    }
    AUDIT_LOGS {
        UUID id PK
        BIGINT user_id FK "set null on user delete"
        VARCHAR username
        VARCHAR event_type "LOGIN_SUCCESS, TOKEN_REUSE, ..."
        VARCHAR ip_address
        VARCHAR user_agent
        TEXT details
        BOOLEAN success
        TIMESTAMPTZ timestamp
    }
```

The schema is created by Flyway migrations `V1` to `V14`. Hibernate only validates it (`ddl-auto: none`).

---

# Authentication flows

## Login

```mermaid
sequenceDiagram
    autonumber
    actor U as Browser
    participant N as nginx
    participant A as auth-service
    participant P as AuthProvider + BCrypt
    participant DB as PostgreSQL

    U->>N: POST /api/v1/auth/login (identifier, password)
    N->>N: rate limit 3 r/s per IP · Lua checks · add X-Request-ID
    N->>A: forward
    A->>P: authenticate(identifier, password)
    P->>DB: find user by email, username or phone
    alt account disabled or locked
        P-->>A: DisabledException or LockedException
    else account active
        P->>P: wait for one of 6 BCrypt permits (max 30 s)
        P->>P: BCrypt check on a virtual thread
    end
    alt bad credentials, disabled or locked
        A->>DB: audit LOGIN_FAILURE
        A-->>U: 401
    else success
        A->>A: new token id (jti) and new family id
        A->>DB: save refresh token (SHA-256 of jti, family, expiry)
        A->>A: sign access token (HS512 · roles · permissions)
        A->>DB: audit LOGIN_SUCCESS
        A-->>U: 200 accessToken + user<br/>Set-Cookie refreshToken (HttpOnly · path /api/v1/auth)<br/>Set-Cookie XSRF-TOKEN · Cache-Control no-store
    end
```

## Authorizing a request

```mermaid
flowchart TD
    req(["Request with Authorization: Bearer token"]) --> sig{"Signature, issuer,<br/>audience valid?"}
    sig -- no --> anon["Continue unauthenticated"]
    sig -- yes --> typ{"type = access<br/>and not expired?"}
    typ -- no --> anon
    typ -- yes --> bl{"jti in Redis<br/>blacklist?"}
    bl -- yes --> revoked["401 Token has been revoked"]
    bl -- no --> principal["Build JwtPrincipal<br/>userId · roles · permissions as authorities"]
    anon --> protected{"Endpoint public?"}
    protected -- no --> unauth["401"]
    protected -- yes --> handler
    principal --> which{"Which controller?"}
    which -- "/api/v1/users" --> pre{"@PreAuthorize<br/>admin permission, or<br/>own permission and id = own userId"}
    which -- "/api/v1/admin" --> rp{"@RequiresPermission<br/>has every listed permission?"}
    which -- "/api/v1/auth" --> handler["Controller runs"]
    pre -- yes --> handler
    pre -- no --> forbidden["403"]
    rp -- yes --> handler
    rp -- no --> forbidden
```

## Refresh token rotation and reuse detection

```mermaid
sequenceDiagram
    autonumber
    actor U as Browser
    participant N as nginx
    participant A as auth-service
    participant DB as PostgreSQL

    U->>N: POST /api/v1/auth/refresh<br/>Cookie refreshToken + XSRF-TOKEN · Header X-XSRF-TOKEN
    N->>A: rate limit 3 r/s · Lua checks
    A->>A: CSRF check (cookie value = header value)
    A->>A: verify JWT signature and type = refresh
    A->>DB: find token by SHA-256(jti)
    alt not found, or belongs to another user
        A->>DB: audit TOKEN_REFRESH_FAILURE
        A-->>U: 401
    else expired
        A->>DB: revoke the whole family
        A-->>U: 401
    else found and not expired
        A->>DB: UPDATE revoked = true WHERE jti_hash = ? AND revoked = false
        alt 0 rows updated: token was already used
            A->>DB: audit TOKEN_REUSE
            A->>DB: revoke the whole family
            A-->>U: 401 (attacker and victim must both log in again)
        else 1 row updated
            A->>DB: load user with roles and permissions
            alt disabled, locked or credentials expired
                A->>DB: revoke the whole family
                A-->>U: 401
            else active
                A->>DB: save new refresh token in the same family
                A-->>U: 200 new accessToken<br/>Set-Cookie new refreshToken
            end
        end
    end
```

## Logout and logout from all devices

```mermaid
sequenceDiagram
    autonumber
    actor U as Browser
    participant A as auth-service
    participant DB as PostgreSQL
    participant R as Redis
    participant K as Kafka
    participant NS as notification-service

    alt POST /api/v1/auth/logout (refresh cookie + CSRF)
        U->>A: logout
        A->>DB: find token by SHA-256(jti), revoke its family
    else POST /api/v1/auth/logout/all (Bearer + CSRF)
        U->>A: logout from all devices
        A->>DB: revoke every refresh token of the user
    end
    A->>R: blacklist current access token jti (TTL = its remaining lifetime)
    A->>A: clear refreshToken cookie
    A->>DB: audit LOGOUT
    A->>K: SESSION_REVOKED (key = userId)
    A-->>U: 204
    K->>NS: AccountEventConsumer
    NS-->>U: SSE event SESSION_REVOKED on every open tab
    NS->>NS: after 1 s close the user's SSE streams and WebSocket sessions
```

## Google sign-in

```mermaid
sequenceDiagram
    autonumber
    actor U as Browser
    participant G as Google
    participant A as auth-service
    participant DB as PostgreSQL

    U->>G: Sign in with Google
    G-->>U: ID token
    U->>A: POST /api/v1/auth/oauth2/google (ID token)
    A->>G: verify signature, audience = GOOGLE_CLIENT_ID
    alt email not verified by Google
        A-->>U: rejected
    end
    A->>DB: find user by (GOOGLE, Google subject)
    alt existing Google user
        Note over A: continue
    else a LOCAL account already uses this email
        A-->>U: rejected: sign in with password (no silent account linking)
    else new user
        A->>DB: create user (provider GOOGLE, profile_complete = false)
    end
    alt account disabled
        A-->>U: rejected
    else active
        A->>DB: new refresh token family · audit LOGIN_SUCCESS
        A-->>U: accessToken + refresh cookie + profileComplete flag
    end
    opt profileComplete = false
        U->>A: PATCH /api/v1/auth/oauth2/complete-profile (Bearer)
        A->>DB: save phone, gender, optional username · profile_complete = true
    end
```

---

# Real-time notifications

## SSE connection

```mermaid
sequenceDiagram
    autonumber
    actor U as Browser (EventSource)
    participant N as nginx
    participant NS as notification-service
    participant R as Redis

    U->>N: GET /api/v1/notifications/stream?token=access-token<br/>(Last-Event-ID on reconnect)
    N->>NS: rate limit 10 r/s · buffering off · 24 h timeout
    NS->>NS: validate JWT (HS512 · issuer · audience)
    alt more than 5 streams for this user, or 1000 in total
        NS-->>U: stream closed with an error
    else accepted
        NS-->>U: event connected
        opt Last-Event-ID present
            NS->>R: LRANGE notification:v1:sse:missed:userId
            NS-->>U: replay every event after Last-Event-ID
        end
        loop every 30 s
            NS-->>U: event ping
        end
    end
```

## Account event pipeline

```mermaid
sequenceDiagram
    autonumber
    actor Admin
    participant A as auth-service
    participant DB as PostgreSQL
    participant K as Kafka (account-events)
    participant NS as notification-service
    participant R as Redis
    actor U as User's browser

    Admin->>A: PUT /api/v1/admin/users/42/status (enabled = false)
    A->>DB: is_enable = false · revoke all refresh tokens
    A->>DB: COMMIT
    A->>K: after commit: ACCOUNT_DISABLED (key = 42, header X-Request-ID)
    K->>NS: AccountEventConsumer (group notification-group)
    NS->>NS: put X-Request-ID into the log context
    NS-->>U: SSE event ACCOUNT_DISABLED to every open stream
    NS->>R: RPUSH event · LTRIM to last 50 · EXPIRE 7 days
    NS->>NS: after 1 s close the user's SSE streams and WebSocket sessions
```

## WebSocket session lifecycle

```mermaid
sequenceDiagram
    autonumber
    actor U as Browser (STOMP.js)
    participant N as nginx
    participant H as Handshake interceptor
    participant I as Subscription interceptor
    participant B as Simple broker
    participant M as WebSocketSessionManager

    U->>N: GET /ws/announcements?token=access-token (Upgrade: websocket)
    N->>H: proxy upgrade · 1 h timeout
    H->>H: check Origin against allow-list
    H->>H: decode JWT · require type = access
    alt missing or invalid token
        H-->>U: handshake rejected
    else valid
        H->>M: register session · StompPrincipal(userId, roles, expiresAt)
        U->>B: STOMP CONNECT · heart-beat 10 s
        U->>I: SUBSCRIBE /topic/announcements
        I->>B: allowed for every user
        U->>I: SUBSCRIBE /topic/admin.responses
        alt user is ADMIN
            I->>B: allowed
        else
            I-->>U: ERROR access denied
        end
        U->>I: SEND to /app/... (any other destination is rejected)
    end
    loop every 60 s
        M->>M: close sessions whose access token has expired
    end
    Note over U,M: idle timeout 5 min · max frame 16 KB · one writer per session
```

## Admin announcements

```mermaid
sequenceDiagram
    autonumber
    actor Admin as Admin (STOMP)
    participant C as AnnouncementStompController
    participant K1 as Kafka broadcast-announcements
    participant NS as Every notification-service instance
    participant B as Broker
    actor Users as Subscribed users
    participant K2 as Kafka admin-responses

    Admin->>C: SEND /app/announcement.send (title, message, priority)
    alt not ADMIN, or title over 100 or message over 2000 chars
        C-->>Admin: /user/queue/errors
    else valid
        C->>K1: publish (key = announcementId · acks = all · idempotent)
        K1->>NS: AnnouncementConsumer (own consumer group per instance)
        NS->>NS: skip if this announcementId was seen in the last 24 h
        NS->>B: /topic/announcements
        B-->>Users: ANNOUNCEMENT
    end
    Users->>C: SEND /app/announcement.ack or /app/announcement.dismiss
    C-->>Users: /user/queue/confirmations
    C->>K2: USER_RESPONSE (userId, ACK or DISMISS)
    K2->>NS: admin-response listener
    NS->>B: /topic/admin.responses
    B-->>Admin: who acknowledged or dismissed
    Note over K1,NS: processing fails → retried 3 times, 1 s apart → broadcast-announcements.DLT
```

---

# RBAC

```mermaid
flowchart LR
    user(["User"]) -->|"user_roles"| roles
    subgraph roles["Roles"]
        direction TB
        admin["ADMIN<br/>system role"]
        usr["USER<br/>system role"]
        custom["Custom roles<br/>created at runtime"]
    end
    subgraph perms["Permissions"]
        direction TB
        pa["admin:create · admin:read<br/>admin:update · admin:delete"]
        pu["user:read · user:update · user:delete"]
        pus["users:read · users:update · users:assign-roles"]
        pr["roles:create · roles:read<br/>roles:update · roles:delete"]
        pp["permissions:create · permissions:read<br/>permissions:delete"]
        pau["audit:read · audit:manage"]
    end
    admin -->|"every permission"| perms
    usr -->|"own account only"| pu
    custom -.->|"any subset"| perms
    perms -->|"copied into the access token"| jwt["JWT claims<br/>roles · permissions"]
```

User endpoints combine a permission with an ownership rule:

```java
@PutMapping("/{id}")
@PreAuthorize("hasAuthority('admin:update') or " +
              "(hasAuthority('user:update') and #id == authentication.principal.userId)")
public ResponseEntity<UserResponseDTO> updateUser(@PathVariable Long id, ...) { ... }
```

Admin endpoints use a custom aspect:

```java
@GetMapping("/users")
@RequiresPermission("users:read")
public ResponseEntity<PagedResponse<CreateUserResponseDTO>> getAllUsers(...) { ... }
```

Role lookups are cached in Redis (`auth:roles::*`) and evicted whenever a role or its permissions change. Permission changes take effect on the user's next token refresh.

---

# Tech stack

| Area | Technology |
|---|---|
| Backend | Java 21 (virtual threads), Spring Boot 4.1, Spring Security, Spring Data JPA / Hibernate, Spring Kafka, Spring WebSocket (STOMP), Maven |
| Database & cache | PostgreSQL (Amazon RDS), HikariCP, Flyway, Redis |
| Messaging | Apache Kafka (KRaft, 2 nodes), Kafka UI, Prometheus Kafka exporter |
| Security | JJWT (HS512), BCrypt, HTTP-only cookies + CSRF, Google OAuth2, RBAC |
| Gateway | OpenResty, Nginx, Lua, Let's Encrypt |
| Frontend | React 19, Vite, React Router, STOMP.js |
| Infrastructure | Docker, Docker Compose, AWS EC2, Amazon RDS, Amazon ECR, AWS SSM |
| CI/CD & quality | GitHub Actions, JaCoCo, SonarCloud, Trivy |
| Testing | JUnit 5, Spring Boot Test, H2 integration tests, Vitest + Testing Library, k6 |

---

# Project structure

```text
AuthService/
├── auth-service/                 # Authentication & authorization API
│   ├── src/main/java/com/lodhi/auth/
│   │   ├── audit/                # Audit events and log
│   │   ├── config/               # Security, Kafka, Redis, BCrypt executor
│   │   ├── controllers/          # Auth, OAuth2, User, Admin
│   │   ├── filters/              # LoggingFilter
│   │   ├── security/             # JWT, cookies, @RequiresPermission
│   │   ├── services/             # Business logic
│   │   └── respositories/        # Spring Data repositories
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   └── db/migration/         # Flyway V1–V14
│   ├── Dockerfile · docker-compose.yml · .env.example
├── notification-service/         # Kafka consumers, SSE + STOMP push
├── notification-contract/        # Shared Kafka event classes
├── client/                       # React + Vite frontend
├── nginx/                        # OpenResty gateway: conf.d/, lua/, ssl/
├── kafka/                        # 2-node KRaft cluster, UI, exporter
├── redis/                        # Redis config and persistence
├── loadtest/                     # k6 scripts and results
├── .github/
│   ├── workflows/ecr-push.yml    # CI/CD pipeline
│   └── scripts/                  # SSM runner + public smoke test
├── CHANGELOG.md
└── README.md
```

---

# API endpoints

## Authentication: `/api/v1/auth`

| Method | Endpoint | Description | Authentication |
| ------ | -------- | ----------- | -------------- |
| `POST` | `/register` | Register user | Public |
| `POST` | `/login` | Login with email, username or phone | Public |
| `POST` | `/refresh` | Rotate refresh token | Refresh cookie + `X-XSRF-TOKEN` |
| `POST` | `/logout` | Revoke current session | Refresh cookie + `X-XSRF-TOKEN` |
| `POST` | `/logout/all` | Revoke all user sessions | Bearer + `X-XSRF-TOKEN` |
| `POST` | `/oauth2/google` | Sign in with Google | Public |
| `PATCH` | `/oauth2/complete-profile` | Finish Google sign-up | Bearer |

## Users: `/api/v1/users`

| Method | Endpoint | Description | Authorization |
| ------ | -------- | ----------- | ------------- |
| `POST` | `/create-user` | Create user | `admin:create` |
| `GET` | `/getByEmail` | Find user by email | `admin:read` / `user:read` |
| `GET` | `/{id}` | Get user | `admin:read` or owner |
| `PUT` | `/{id}` | Update user | `admin:update` or owner |
| `DELETE` | `/{id}` | Delete user | `admin:delete` or owner |
| `POST` | `/{id}/change-password` | Change password | Owner only |

## Administration: `/api/v1/admin`

| Method | Endpoint | Description | Permission |
| ------ | -------- | ----------- | ---------- |
| `GET` | `/roles` | List roles | `roles:read` |
| `POST` | `/roles` | Create role | `roles:create` |
| `DELETE` | `/roles/{roleId}` | Delete role | `roles:delete` |
| `POST` | `/roles/{roleId}/permissions` | Assign permissions | `roles:update` |
| `DELETE` | `/roles/{roleId}/permissions` | Revoke permissions | `roles:update` |
| `GET` | `/permissions` | List permissions | `permissions:read` |
| `POST` | `/permissions` | Create permission | `permissions:create` |
| `DELETE` | `/permissions/{permissionId}` | Delete permission | `permissions:delete` |
| `GET` | `/users?page=&size=&sortBy=` | List users (paginated) | `users:read` |
| `PUT` | `/users/{userId}/roles` | Assign roles | `users:assign-roles` |
| `PUT` | `/users/{userId}/status` | Enable / disable user | `users:update` |

## Notifications

| Type | Endpoint | Description |
| ---- | -------- | ----------- |
| SSE | `GET /api/v1/notifications/stream` | Live account events (`?token=` or Bearer, `Last-Event-ID` to resume) |
| WebSocket | `/ws/announcements?token=` | STOMP endpoint |
| STOMP | `SUBSCRIBE /topic/announcements` | Receive announcements |
| STOMP | `SUBSCRIBE /topic/admin.responses` | Ack and dismiss responses (admin only) |
| STOMP | `SUBSCRIBE /user/queue/confirmations` · `/user/queue/errors` | Personal confirmations and errors |
| STOMP | `SEND /app/announcement.send` | Broadcast an announcement (admin) |
| STOMP | `SEND /app/announcement.ack` · `/app/announcement.dismiss` | Acknowledge or dismiss |

---

# Environment configuration

Copy the example file. notification-service reads the same file, so both services share the JWT secret, issuer and audience.

```bash
cp auth-service/.env.example auth-service/.env
```

Never commit `.env` or any file containing real secrets.

| Group | Variables |
|---|---|
| Database | `DB_HOST` `DB_PORT` `DB_NAME` `DB_USERNAME` `DB_PASSWORD` |
| Admin bootstrap | `ADMIN_EMAIL` `ADMIN_USERNAME` `ADMIN_PASSWORD` `ADMIN_PHONE` |
| JWT | `JWT_SECRET` (≥ 64 bytes) `JWT_ISSUER` `JWT_AUDIENCE` `JWT_ACCESS_TTL_SECOND` `JWT_REFRESH_TTL_SECOND` |
| Refresh cookie | `JWT_REFRESH_TOKEN_COOKIE_NAME` `JWT_COOKIE_HTTP_ONLY` `JWT_COOKIE_SECURE` `JWT_COOKIE_SAME_SITE` `JWT_COOKIE_DOMAIN` `JWT_COOKIE_PATH` |
| Google | `GOOGLE_CLIENT_ID` |
| Redis | `REDIS_HOST` `REDIS_PORT` `REDIS_PASSWORD` `CACHE_TTL` |
| Kafka | `KAFKA_BOOTSTRAP_SERVERS` `KAFKA_TOPIC_REPLICATION_FACTOR` |
| HikariCP | `HIKARI_MAX_POOL_SIZE` `HIKARI_MIN_IDLE` `HIKARI_CONNECTION_TIMEOUT` `HIKARI_IDLE_TIMEOUT` `HIKARI_MAX_LIFETIME` `HIKARI_LEAK_DETECTION_THRESHOLD` |
| Application | `SERVER_PORT` `SERVER_CONNECTION_TIMEOUT` `QUERY_TIMEOUT` `LOCK_TIMEOUT` `BCRYPT_MAX_CONCURRENT` |
| notification-service | `CORS_ALLOWED_ORIGINS` `WEBSOCKET_ALLOWED_ORIGINS` |
| Logging / Actuator | `LOG_LEVEL_*` `LOG_FILE` `ACTUATOR_ENDPOINTS` `ACTUATOR_HEALTH_SHOW_DETAILS` |
| Frontend (build time) | `VITE_API_URL` `VITE_GOOGLE_CLIENT_ID` |
| Development only | `JPA_SHOW_SQL` `JPA_FORMAT_SQL` |

---

# Running locally

**Prerequisites:** Java 21, Maven 3.9+, Docker with Compose, Node.js 20, PostgreSQL 15+.

```bash
git clone https://github.com/lodhiPlayBits/AuthService.git
cd AuthService

# 1. Shared Docker network
docker network create authnet

# 2. Redis and Kafka (Kafka creates authvolt-kafka-network)
(cd redis && docker compose up -d)
(cd kafka && docker compose up -d)

# 3. Shared event contract (both services depend on it)
mvn -f notification-contract/pom.xml -DskipTests install

# 4. Configuration
cp auth-service/.env.example auth-service/.env     # fill in the values

# 5. Build the jars, then start the services
mvn -f auth-service/pom.xml -DskipTests package
mvn -f notification-service/pom.xml -DskipTests package
(cd auth-service && docker compose --env-file .env up -d --build)
(cd notification-service && docker compose up -d --build)

# 6. Frontend
cd client && npm ci && npm run dev
```

Flyway creates the database schema on first start. Check containers with `docker ps` and logs with `docker logs -f auth-service`.

---

# Testing

```bash
mvn -f auth-service/pom.xml clean verify          # report: auth-service/target/site/jacoco/index.html
mvn -f notification-service/pom.xml clean verify
(cd client && npm test)
```

The suites cover unit tests, Spring context tests, controller, filter and security tests, repository and integration tests (H2), and frontend component tests.

Load tests live in [`loadtest/`](loadtest). Moving BCrypt onto virtual threads behind a semaphore gave **+28% throughput, −29% p95 login latency and zero timeouts** at 40 RPS on a 2-vCPU t3.large.

---

# Deployment

## deploy.sh on the EC2 host

```mermaid
flowchart TD
    start(["SSM runs deploy.sh with the commit SHA"]) --> login["docker login to ECR<br/>instance role, no stored keys"]
    login --> order["Pull and start images, in order<br/>redis → kafka → auth-service →<br/>notification-service → client → nginx"]
    order --> healthy{"Every container healthy<br/>within 300 s?"}
    healthy -- yes --> smoke{"Smoke tests pass?<br/>/health reports UP · bad login → 401<br/>stream without token → 401<br/>/client/ → 200 · bundle uses prod API URL"}
    healthy -- no --> failed["Deploy failed"]
    smoke -- yes --> good["Record SHA as last good tag"]
    good --> ok(["exit 0 · pipeline continues to public smoke test"])
    smoke -- no --> failed
    failed --> prev{"Last good tag<br/>recorded?"}
    prev -- no --> manual(["exit 1 · manual action needed"])
    prev -- yes --> rollback["Redeploy last good tag<br/>same health and smoke checks"]
    rollback --> rbok{"Rollback healthy?"}
    rbok -- yes --> red(["exit 1 · site runs previous version · pipeline red"])
    rbok -- no --> manual
```

## Pipeline triggers

| Event | What runs |
| ----- | --------- |
| Pull request | Tests, scans, Sonar, image build (no push) |
| Push to `develop` | Everything above + push images to ECR |
| Push to `main` | Everything above + deploy + public smoke test (+ rollback and revert PR on failure) |

`main` and `develop` are protected, so every change goes through a pull request. Production configuration comes from environment variables. Never hardcode secrets in source code, Dockerfiles, Git history or committed configuration.

---

# Security considerations

```mermaid
flowchart TB
    subgraph edge["1 · Edge: nginx"]
        direction LR
        e1["TLS · HSTS<br/>security headers"] --> e2["Rate limits<br/>20 connections per IP"] --> e3["Lua checks<br/>Content-Type · Bearer present"]
    end
    subgraph app["2 · Application: Spring Security"]
        direction LR
        a1["JWT checks<br/>HS512 · iss · aud · exp · type"] --> a2["Redis blacklist"] --> a3["CSRF on<br/>cookie endpoints"] --> a4["Permissions<br/>and ownership"] --> a5["Audit log"]
    end
    subgraph tokens["3 · Tokens"]
        direction LR
        t1["Short-lived<br/>access token"] --> t2["Rotating refresh token<br/>hashed at rest"] --> t3["Reuse →<br/>family revoked"] --> t4["Logout → blacklist<br/>+ sessions closed"]
    end
    subgraph data["4 · Data"]
        direction LR
        d1["Hikari pool limits"] --> d2["Query timeout 10 s<br/>lock timeout 5 s"] --> d3["Flyway-validated<br/>schema"]
    end
    subgraph supply["5 · Supply chain"]
        direction LR
        s1["Trivy filesystem<br/>and image scans"] --> s2["SonarCloud"] --> s3["PR-only branches<br/>OIDC · SSM, no SSH"]
    end
    edge --> app --> tokens --> data
    data ~~~ supply
```

No single control is expected to protect the whole system.

---

# Design goals

* Secure authentication and fine-grained authorization
* Stateless access tokens, stateful and revocable refresh tokens
* Refresh-token compromise detection
* Defense in depth at the gateway and application layers
* Event-driven communication between services
* Real-time notifications that survive reconnects
* Production-oriented database configuration
* Traceable requests across services
* Containerized, automated deployment with safe rollback

---

# Future improvements

* Publish `ROLE_CHANGED`, `PASSWORD_CHANGED` and `ACCOUNT_DELETED` events (already in the contract)
* Email verification and password reset
* Account lockout / suspicious-login detection
* MFA / TOTP
* Redis-backed distributed rate limiting
* Distributed tracing with OpenTelemetry
* Prometheus + Grafana dashboards
* Secret management with AWS Secrets Manager
* Asymmetric JWT signing (RS256 / JWKS)

---

# Versioning

The project follows [Semantic Versioning](https://semver.org/). Releases are tagged `vX.Y.Z` on `main` and described in the [CHANGELOG](CHANGELOG.md). Commit messages follow [Conventional Commits](https://www.conventionalcommits.org/).

---

# Author

**Gaurav Lodhi**

Backend / Software Engineer focused on Java, Spring Boot, distributed systems, databases, and production backend infrastructure.

If you find the project useful, consider giving it a ⭐.

Built with **Java 21 + Spring Boot + PostgreSQL + Redis + Kafka + OpenResty** · Licensed under [MIT](LICENSE).
