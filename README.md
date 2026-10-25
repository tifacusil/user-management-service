# User Management Service

Microservice responsible for user authentication, profile management, alert subscriptions, and device token registration in the **Landslide Monitoring System** (Bai et al., 2020).

- **Port:** `8085`
- **Stack:** Java 17 · Spring Boot 3.2.5 · MyBatis · MySQL · Consul

---

## Table of Contents

- [Prerequisites](#prerequisites)
- [Getting Started](#getting-started)
- [Configuration](#configuration)
- [Running the Service](#running-the-service)
- [API Reference](#api-reference)
- [Running Tests](#running-tests)
- [Build](#build)
- [Architecture Notes](#architecture-notes)

---

## Prerequisites

| Tool | Version | Notes |
|---|---|---|
| Java JDK | 17 | JRE alone is not enough — you need `javac` |
| Maven | — | Included via `./mvnw` wrapper (auto-downloads Maven 3.9.6) |
| MySQL | 8.x | Tables must already exist in `landslide_monitoring` |
| Consul | 1.x | Running at the address specified in `.env` |

Install the JDK on Ubuntu/Debian:

```bash
sudo apt-get install -y openjdk-17-jdk-headless
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
```

---

## Getting Started

```bash
# Clone / enter the project
cd user-management-service

# Copy the environment file and edit as needed
cp .env.example .env   # then fill in the secrets

# Compile
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ./mvnw compile
```

---

## Configuration

All configuration is driven by environment variables. The service reads a `.env` file from the working directory at startup (via `dotenv-java`), then falls back to real environment variables.

| Variable | Default | Description |
|---|---|---|
| `SERVER_PORT` | `8085` | HTTP port |
| `DB_URL` | `jdbc:mysql://204.168.203.32:3306/landslide_monitoring` | MySQL JDBC URL |
| `DB_USERNAME` | `landslide` | MySQL user |
| `DB_PASSWORD` | — (required) | MySQL password |
| `JWT_SECRET` | — (required) | HS256 signing key (≥ 32 chars) |
| `CONSUL_HOST` | `204.168.203.32` | Consul agent host |
| `CONSUL_PORT` | `8500` | Consul agent port |
| `APP_HOST` | `localhost` | IP Consul uses to reach this service's `/health` endpoint |
| `APP_ENV` | `development` | Injected into Consul service tags |

> **Note on `JWT_SECRET`:** The key is zero-padded internally if it is shorter than 32 bytes. For production, use a randomly generated secret of at least 32 characters.

> **Note on Consul ACLs:** If Consul has ACL enabled, add `CONSUL_TOKEN=<acl_token>` to `.env` and add the `X-Consul-Token` header in `ConsulConfig.java`.

---

## Running the Service

```bash
# Development (reads .env automatically)
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 \
  java -jar target/user-management-1.0.0.jar

# Or via Maven
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ./mvnw spring-boot:run
```

The service registers itself with Consul on startup and is reachable at `http://localhost:8085`.

---

## API Reference

All endpoints except `/health` and `/api/v1/auth/**` require `Authorization: Bearer <JWT>`.

---

### Authentication

#### `POST /api/v1/auth/register`

Create a new user. Only an authenticated `admin` can create another `admin`.

**Request**
```json
{
  "username": "operador1",
  "email": "op@test.com",
  "phone_e164": "+51999000000",
  "password": "secret123",
  "role": "operator"
}
```
Roles: `admin` · `operator` · `viewer`

**Response `201`**
```json
{ "user_id": 6, "username": "operador1", "role": "operator" }
```

**Errors:** `400` validation · `403` non-admin trying to create admin · `409` username/email already exists

---

#### `POST /api/v1/auth/login`

Authenticate and obtain a JWT (valid 24 h).

**Request**
```json
{ "username": "admin", "password": "<password>" }
```

**Response `200`**
```json
{
  "token": "<JWT>",
  "user_id": 1,
  "username": "admin",
  "role": "admin",
  "expires_at": "2026-05-18T14:30:00Z"
}
```

**Response `401`** — same message for wrong password and unknown username (timing-safe).

---

### Users

#### `GET /api/v1/users/{user_id}`

Returns a user profile. Admins can fetch any user; operators/viewers can only fetch themselves.

**Response `200`**
```json
{
  "user_id": 1,
  "username": "admin",
  "email": "admin@landslide.test",
  "phone_e164": "+51999111111",
  "role": "admin",
  "status": "active",
  "created_at": "2026-05-17T14:00:00Z"
}
```

**Errors:** `403` accessing another user · `404` user not found

---

#### `PUT /api/v1/users/{user_id}`

Update `email` and/or `phone_e164`. `username`, `role`, and `status` are not modifiable via this endpoint.

**Request**
```json
{ "phone_e164": "+51999000111", "email": "nuevo@test.com" }
```

**Response `200`** — updated user profile (same shape as GET).

---

### Subscriptions

#### `POST /api/v1/users/{user_id}/subscriptions`

Subscribe a user to a project's alerts. **Admin only.**

**Request**
```json
{
  "project_id": "shangtan",
  "min_alert_level": "blue",
  "sms_enabled": true,
  "push_enabled": true,
  "email_enabled": false,
  "quiet_hours_start": "22:00",
  "quiet_hours_end": "06:00"
}
```
Alert levels: `blue` < `orange` < `red`. `quiet_hours_*` are optional; the range can cross midnight.

**Response `201`** — the created subscription.

**Errors:** `403` non-admin · `404` project or user not found · `409` subscription already exists

---

#### `GET /api/v1/users/{user_id}/subscriptions`

List all subscriptions for a user.

**Response `200`**
```json
[
  {
    "id": 1,
    "project_id": "shangtan",
    "min_alert_level": "blue",
    "sms_enabled": true,
    "push_enabled": true,
    "email_enabled": true,
    "quiet_hours_start": "22:00",
    "quiet_hours_end": "06:00"
  }
]
```

---

### Devices

#### `POST /api/v1/devices/register`

Register or refresh a push notification device token for the authenticated user.

**Request**
```json
{ "device_token": "fcm_token_xyz", "platform": "android", "app_version": "1.0.0" }
```

**Response `201`** (new token) or `200` (same user, token refreshed):
```json
{ "device_id": 5 }
```

If the token already belongs to **another** user, ownership is transferred and the calling user receives it.

---

#### `POST /api/v1/devices/{device_token}/revoke`

Mark a device token as revoked. Called by the Early Warning service when FCM reports a token as invalid.

**Request**
```json
{ "reason": "fcm_unregistered" }
```

**Response `200`**
```json
{ "revoked": true }
```

**Errors:** `404` token not found

---

### Alert Recipients *(consumed by Early Warning service)*

#### `GET /api/v1/projects/{project_id}/alert-recipients?level={blue|orange|red}`

Returns all users who should be notified for an alert at the given level, with per-channel details.

**Response `200`**
```json
{
  "project_id": "shangtan",
  "alert_level": "orange",
  "recipients": [
    {
      "user_id": 1,
      "username": "admin",
      "role": "admin",
      "channels": {
        "sms": {
          "enabled": true,
          "phone_e164": "+51999111111",
          "suppressed_by_quiet_hours": false
        },
        "push": {
          "enabled": true,
          "devices": [
            { "token": "fcm_token_admin_android", "platform": "android" }
          ]
        },
        "email": {
          "enabled": true,
          "address": "admin@landslide.test"
        }
      }
    }
  ],
  "generated_at": "2026-05-17T14:30:00Z"
}
```

**Filtering rules:**
1. Only users with `status = active`.
2. Only subscriptions where `min_alert_level ≤ requested level` (blue=1, orange=2, red=3).
3. `suppressed_by_quiet_hours = true` when the current UTC time falls within the user's quiet window **and** `level ≠ red`. Red alerts are **never** suppressed.
4. Quiet-hour windows that cross midnight (e.g. 22:00–06:00) are handled correctly.
5. Only device tokens with `status = active` appear in `push.devices`.
6. Empty `recipients` returns `[]` with HTTP 200 — not an error.

---

### Health

#### `GET /health`

No authentication required. Polled by Consul every 10 seconds.

**Response `200`**
```json
{ "service": "user-management", "status": "UP", "db": "UP" }
```

**Response `503`** — when MySQL is unreachable.

---

## Running Tests

Integration tests connect to the **real database** and create uniquely-named test users (timestamp suffix) to avoid conflicts across runs.

```bash
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ./mvnw test
```

| # | Test | Covers |
|---|---|---|
| 1 | `register_createsUser_withHashedPassword` | BCrypt hash stored, not plain text |
| 2 | `login_withCorrectCredentials_returnsJwtWithClaims` | JWT contains `username` and `role` claims |
| 3 | `login_withWrongPassword_returns401` | No credential enumeration |
| 4 | `alertRecipients_orange_includesBlueAndOrange_excludesRedOnly` | Level filtering |
| 5 | `alertRecipients_red_includesAllSubscribers` | Red includes all levels |
| 6 | `alertRecipients_orange_at23Utc_operadorSuppressed` | Quiet hours active for non-red |
| 7 | `alertRecipients_red_at23Utc_operadorNotSuppressed` | Red bypasses quiet hours |
| 8 | `registerDevice_tokenOfOtherUser_revokesOldAndCreatesNew` | Token ownership transfer |
| 9 | `health_returns200_whenDbIsUp` | Health endpoint + DB connectivity |

---

## Build

```bash
# Build fat JAR
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ./mvnw package -DskipTests
```

---

## Architecture Notes

- **No message broker** — this service does not connect to RabbitMQ. SMS and push delivery are handled by the Early Warning service; this service only stores and serves recipient data.
- **No DDL** — tables are pre-existing. The service never creates or modifies the schema.
- **SQL injection prevention** — all MyBatis queries use `#{}` parameter binding exclusively.
- **Password security** — `password_hash` is BCrypt strength 10. It is never serialized in any JSON response (models and DTOs are separate).
- **Consul registration** — performed at `ApplicationReadyEvent` via the Consul HTTP API (`PUT /v1/agent/service/register`). A warning is logged (not a fatal error) if Consul is unreachable.
# user-management-service
