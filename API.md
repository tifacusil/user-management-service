# User Management Service — Flutter API Reference

Base URL: `http://YOUR_SERVER_IP:8085`

> **All fields use `snake_case`** (Jackson serializes that way).  
> All endpoints except `/health` and `/api/v1/auth/**` require a JWT in the header:
> ```
> Authorization: Bearer <token>
> ```

---

## Table of Contents

- [Authentication](#authentication)
  - [Register](#post-apiv1authregister)
  - [Login](#post-apiv1authlogin)
- [Users](#users)
  - [Get user](#get-apiv1usersuserid)
  - [Update user](#put-apiv1usersuserid)
- [Subscriptions](#subscriptions)
  - [Create subscription](#post-apiv1usersuseridsubscriptions)
  - [List subscriptions](#get-apiv1usersuseridsubscriptions)
- [Devices](#devices)
  - [Register device token](#post-apiv1devicesregister)
  - [Revoke device token](#post-apiv1devicesdevicetokenrevoke)
- [Alert Recipients](#alert-recipients)
  - [Get recipients](#get-apiv1projectsprojectidaleert-recipients)
- [Health](#health)
- [Error Responses](#error-responses)
- [Flutter Integration Tips](#flutter-integration-tips)

---

## Authentication

### `POST /api/v1/auth/register`

Creates a new user account. No JWT required.

- Roles: `admin` · `operator` · `viewer`
- Creating an `admin` account requires the caller to be an authenticated admin (pass the JWT).
- Default role is `operator` if omitted.

**Request**
```json
{
  "username": "operador1",
  "email": "op@example.com",
  "phone_e164": "+51999000000",
  "password": "secret123",
  "role": "operator"
}
```

| Field | Type | Required | Rules |
|---|---|---|---|
| `username` | string | ✅ | 3–50 chars |
| `email` | string | ✅ | valid email format |
| `phone_e164` | string | ❌ | E.164 format, e.g. `+51999000000` |
| `password` | string | ✅ | min 6 chars |
| `role` | string | ❌ | `admin` / `operator` / `viewer` (default: `operator`) |

**Response `201`**
```json
{
  "user_id": 6,
  "username": "operador1",
  "role": "operator"
}
```

**Errors**

| Code | Reason |
|---|---|
| `400` | Validation failed (blank fields, bad email, short password) |
| `403` | Non-admin trying to create an admin account |
| `409` | Username or email already exists |

---

### `POST /api/v1/auth/login`

Returns a JWT valid for **24 hours**.

**Request**
```json
{
  "username": "admin",
  "password": "password123"
}
```

**Response `200`**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "user_id": 1,
  "username": "admin",
  "role": "admin",
  "expires_at": "2026-05-26T01:00:00Z"
}
```

> Store `token`, `user_id`, and `role` locally (e.g. `shared_preferences`). Attach the token to every subsequent request.

**Errors**

| Code | Reason |
|---|---|
| `401` | Wrong password or unknown username (same message for both — no enumeration) |

---

## Users

### `GET /api/v1/users/{user_id}`

Returns a user profile. **Admins** can fetch any user. **Operators/viewers** can only fetch themselves.

**Response `200`**
```json
{
  "user_id": 1,
  "username": "admin",
  "email": "admin@example.com",
  "phone_e164": "+51999111111",
  "role": "admin",
  "status": "active",
  "created_at": "2026-05-17T14:00:00Z"
}
```

**Errors**

| Code | Reason |
|---|---|
| `403` | Trying to access another user's profile without admin role |
| `404` | User not found |

---

### `PUT /api/v1/users/{user_id}`

Updates `email` and/or `phone_e164`. Username, role, and status cannot be changed here.

**Request** *(send only the fields you want to update)*
```json
{
  "email": "nuevo@example.com",
  "phone_e164": "+51999000111"
}
```

**Response `200`** — updated user profile (same shape as GET)

**Errors**

| Code | Reason |
|---|---|
| `403` | Updating another user without admin role |
| `404` | User not found |

---

## Subscriptions

### `POST /api/v1/users/{user_id}/subscriptions`

Subscribes a user to project alerts. **Admin only.**

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

| Field | Type | Required | Notes |
|---|---|---|---|
| `project_id` | string | ✅ | project identifier |
| `min_alert_level` | string | ✅ | `blue` / `orange` / `red` |
| `sms_enabled` | boolean | ✅ | |
| `push_enabled` | boolean | ✅ | |
| `email_enabled` | boolean | ✅ | |
| `quiet_hours_start` | string | ❌ | `HH:MM` format, UTC |
| `quiet_hours_end` | string | ❌ | `HH:MM` format, UTC — can cross midnight |

Alert level hierarchy: `blue` < `orange` < `red`  
(A subscription with `min_alert_level: blue` receives blue, orange, and red alerts.)

**Response `201`**
```json
{
  "id": 1,
  "project_id": "shangtan",
  "min_alert_level": "blue",
  "sms_enabled": true,
  "push_enabled": true,
  "email_enabled": false,
  "quiet_hours_start": "22:00",
  "quiet_hours_end": "06:00"
}
```

**Errors**

| Code | Reason |
|---|---|
| `403` | Caller is not an admin |
| `404` | User or project not found |
| `409` | Subscription already exists for this user + project |

---

### `GET /api/v1/users/{user_id}/subscriptions`

Lists all subscriptions for a user. Admins can list any user's subscriptions; others can only list their own.

**Response `200`**
```json
[
  {
    "id": 1,
    "project_id": "shangtan",
    "min_alert_level": "blue",
    "sms_enabled": true,
    "push_enabled": true,
    "email_enabled": false,
    "quiet_hours_start": "22:00",
    "quiet_hours_end": "06:00"
  }
]
```

Returns `[]` if the user has no subscriptions.

**Errors**

| Code | Reason |
|---|---|
| `403` | Accessing another user's subscriptions without admin role |

---

## Devices

### `POST /api/v1/devices/register`

Registers or refreshes a push notification token for the authenticated user. Call this on every app launch after login.

**Request**
```json
{
  "device_token": "fcm_token_xyz_abc",
  "platform": "android",
  "app_version": "1.0.0"
}
```

| Field | Type | Required | Notes |
|---|---|---|---|
| `device_token` | string | ✅ | FCM or APNs token |
| `platform` | string | ✅ | `android` / `ios` / `web` |
| `app_version` | string | ❌ | e.g. `1.0.0` |

**Response `201`** — new token registered  
**Response `200`** — token already owned by this user, `last_seen_at` refreshed
```json
{
  "device_id": 5
}
```

> If the token was previously registered to a **different** user, ownership is automatically transferred to the current user.

---

### `POST /api/v1/devices/{device_token}/revoke`

Marks a device token as revoked. Typically called by the Early Warning service when FCM reports a token as invalid — but the Flutter app can also call it on logout.

**Request**
```json
{
  "reason": "user_logout"
}
```

**Response `200`**
```json
{
  "revoked": true
}
```

**Errors**

| Code | Reason |
|---|---|
| `404` | Token not found |

---

## Alert Recipients

### `GET /api/v1/projects/{project_id}/alert-recipients?level={level}`

Returns all users who should be notified for an alert at the given level, with per-channel details. Consumed by the Early Warning service — the Flutter app does **not** normally call this.

**Query parameter:** `level` = `blue` | `orange` | `red`

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
            { "token": "fcm_token_abc", "platform": "android" }
          ]
        },
        "email": {
          "enabled": true,
          "address": "admin@example.com"
        }
      }
    }
  ],
  "generated_at": "2026-05-25T01:30:00Z"
}
```

Returns `"recipients": []` with HTTP 200 if no users match — never a 404.

**Errors**

| Code | Reason |
|---|---|
| `400` | Invalid level (not blue/orange/red) |

---

## Health

### `GET /health`

No authentication required. Polled by Consul every 10 seconds.

**Response `200`**
```json
{
  "service": "user-management",
  "status": "UP",
  "db": "UP"
}
```

**Response `503`** — MySQL unreachable.

---

## Error Responses

All errors return a JSON body:

```json
{
  "error": "Description of what went wrong"
}
```

| Code | Meaning |
|---|---|
| `400` | Bad request / validation error |
| `401` | Missing or invalid JWT (or wrong login credentials) |
| `403` | Valid JWT but insufficient permissions |
| `404` | Resource not found |
| `409` | Conflict (duplicate username, email, or subscription) |
| `500` | Unexpected server error |

---

## Flutter Integration Tips

### 1. Store the token after login

```dart
// Using shared_preferences
await prefs.setString('jwt_token', response.token);
await prefs.setInt('user_id', response.userId);
await prefs.setString('role', response.role);
```

### 2. Attach JWT to every request

```dart
final token = prefs.getString('jwt_token');

final response = await http.get(
  Uri.parse('$baseUrl/api/v1/users/$userId'),
  headers: {
    'Authorization': 'Bearer $token',
    'Content-Type': 'application/json',
  },
);
```

### 3. Register FCM token after login

```dart
final fcmToken = await FirebaseMessaging.instance.getToken();

await http.post(
  Uri.parse('$baseUrl/api/v1/devices/register'),
  headers: {
    'Authorization': 'Bearer $jwt',
    'Content-Type': 'application/json',
  },
  body: jsonEncode({
    'device_token': fcmToken,
    'platform': 'android',  // or 'ios'
    'app_version': '1.0.0',
  }),
);
```

### 4. Revoke token on logout

```dart
final fcmToken = await FirebaseMessaging.instance.getToken();

await http.post(
  Uri.parse('$baseUrl/api/v1/devices/$fcmToken/revoke'),
  headers: {
    'Authorization': 'Bearer $jwt',
    'Content-Type': 'application/json',
  },
  body: jsonEncode({'reason': 'user_logout'}),
);

await prefs.clear(); // clear local session
```

### 5. Handle 401 globally (token expired)

```dart
if (response.statusCode == 401) {
  // Token expired — redirect to login screen
  await prefs.clear();
  Navigator.pushReplacementNamed(context, '/login');
}
```
