# Changelog

All notable changes to this project are documented here.
The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the project uses [Semantic Versioning](https://semver.org/).

## [2.0.0] - 2026-10-11

### Breaking changes
- **Refresh flow is cookie-only.** `POST /api/v1/auth/refresh` no longer reads the refresh token from the request body. It reads the `refreshToken` HttpOnly cookie and requires the `X-XSRF-TOKEN` header (CSRF double-submit).
- **New token format.** Tokens are signed with HS512 and must carry `iss` and `aud`. Tokens issued by v1.0.0 are rejected, so every user has to log in again.
- **`GET /api/v1/users` removed.** Use `GET /api/v1/admin/users?page=&size=&sortBy=` instead. It is paginated and requires the `users:read` permission.
- **`user` object in `TokenResponse` changed.** It adds `username`, `enabled` and `profileComplete`, and renames `Id` to `id`.
- **Schema is managed by Flyway (V1–V14).** Hibernate `ddl-auto: update` is gone. A v1.0.0 database must be recreated or baselined before upgrading.

### Added
- Dynamic roles and permissions with an admin API (`/api/v1/admin/**`)
- Google OAuth2 sign-in (`/api/v1/auth/oauth2/google`, `/complete-profile`)
- Refresh token rotation with token families, `logout` and `logout/all`, and a Redis token blacklist
- `notification-service`: Kafka account events, SSE, and STOMP/WebSocket announcements
- Infrastructure: 2-node Kafka KRaft cluster, Redis, nginx (OpenResty) with TLS and rate limiting
- CI/CD: tests with JaCoCo, Trivy filesystem and image scans, SonarCloud, ECR push that skips unchanged services, SSM deploy, public smoke test, and automatic rollback with a revert PR

## [1.0.0] - 2026-09-11
- Initial auth service: register, login, refresh, and JWT-secured user CRUD.

[2.0.0]: https://github.com/lodhiPlayBits/AuthService/compare/v1.0.0...v2.0.0
[1.0.0]: https://github.com/lodhiPlayBits/AuthService/releases/tag/v1.0.0
