# Authentication and Security

Phase 4 adds stateless authentication to the shared Spring Boot API. Web and mobile clients use the same endpoints; the refresh-token transport differs by client platform.

## Endpoints

All responses use the common `{ "success", "message", "data", "timestamp" }` envelope.

| Method and path | Access | Purpose |
| --- | --- | --- |
| `POST /api/v1/auth/register` | Public | Create a CUSTOMER account and start a session |
| `POST /api/v1/auth/login` | Public | Authenticate with email and password |
| `POST /api/v1/auth/refresh` | Public, refresh credential required | Rotate a refresh token and issue a new access token |
| `POST /api/v1/auth/logout` | Public, refresh credential required | Revoke the refresh-token family |
| `GET /api/v1/auth/me` | Bearer access token | Return the authenticated user's safe profile |
| `POST /api/v1/auth/forgot-password` | Public | Send password reset instructions |
| `POST /api/v1/auth/reset-password` | Public, reset token required | Set a new password and revoke sessions |
| `POST /api/v1/admin/users` | `USER_CREATE` permission | Create an account with permitted role assignments |
| `GET /api/v1/categories/**` | Public catalog data | Read active categories |
| `GET /api/v1/costumes/**` | Public catalog data | Read active costumes and available variants |
| `GET /api/v1/health` | Public | Check backend health |

### Register and login

Example registration request:

```json
{
  "fullName": "A customer",
  "email": "customer@example.test",
  "phoneNumber": "+1 555 0100",
  "password": "StrongPass1!"
}
```

Passwords must be 8–72 UTF-8 bytes and include uppercase, lowercase, numeric, and non-whitespace special characters. Passwords are stored with BCrypt. Registration always assigns only the CUSTOMER role. Login failures use a generic unauthorized response.

Successful login and registration return an access token, expiry in seconds, and safe user profile. Refresh-token delivery depends on the client:

- **Web:** omit `X-Client-Platform` or send `web`. The access token is returned in JSON; refresh and CSRF tokens are set as cookies.
- **Mobile:** send `X-Client-Platform: mobile`. Both access and refresh tokens are returned in JSON. Store refresh tokens only in the platform's secure storage.

Send access tokens as `Authorization: Bearer <access-token>`. Access tokens are short lived (15 minutes by default). They contain a UUID subject, token type, `jti`, issue/expiry time, and issuer; user profile information is not embedded.

### Refresh and logout

For web requests, the API reads the `refresh_token` HttpOnly cookie and requires the `csrf_token` cookie value to match the `X-CSRF-TOKEN` request header. Both cookies use `SameSite=Strict` and are Secure when `AUTH_COOKIE_SECURE=true`. The refresh cookie is scoped to `/api/v1/auth`; the readable CSRF cookie uses path `/` so the web application can copy it into the header. When issuing a session, the backend expires any older CSRF cookie scoped to `/api/v1/auth`. Refresh rotates the refresh cookie and replaces the CSRF cookie. Logout revokes the active token family and clears both cookie paths.

For browser auth requests, a supplied `Origin` must exactly match one of `CORS_ALLOWED_ORIGINS`; do not configure wildcard origins when credentials are enabled.

For mobile requests, send `X-Client-Platform: mobile` and provide `refreshToken` in the JSON body. On successful refresh, replace the stored refresh token immediately. Logout accepts the same body shape.

Refresh tokens are signed JWTs, but only their SHA-256 hashes are stored in the database. A refresh-token family is recorded for each login. Rotation revokes the presented token and records its replacement. A per-user database lock serializes refresh, logout, login session creation, and password-reset revocation; each operation commits its token changes atomically. Reuse of a revoked token is rejected. Reuse after a five-second concurrency grace period revokes every active token in that family; within the grace period the duplicate request is rejected without revoking the replacement session, preventing simultaneous legitimate requests from logging the user out. Clients should still serialize refresh requests (single-flight) and store a successful replacement immediately. Logout revokes the entire family. Expired, malformed, wrong-type, or revoked tokens are rejected.

Logout and password reset revoke refresh sessions. Already-issued access tokens are intentionally short-lived and remain valid until their expiry (15 minutes by default); clients should discard their local access token immediately when logging out or resetting a password.

### Password recovery

`POST /api/v1/auth/forgot-password` accepts `{ "email": "..." }`. It returns the same generic message whether the account exists or not. For existing active accounts, a random, short-lived, single-use token is generated and only its hash is persisted.

With `PASSWORD_RESET_DELIVERY=smtp`, the token is sent through configured SMTP and is never included in the API response. With the explicit local-only setting `PASSWORD_RESET_DELIVERY=development`, the raw token is included as `data.developmentResetToken` for testing; do not enable this mode in production.

`POST /api/v1/auth/reset-password` accepts:

```json
{
  "token": "one-time-token",
  "newPassword": "NewStrongPass2!"
}
```

The reset token is single-use and expires after 15 minutes by default. A successful reset revokes all refresh sessions for that user.

### Admin user creation and authorization

`POST /api/v1/admin/users` requires `USER_CREATE` and accepts `fullName`, `email`, optional `phoneNumber`, strong `password`, and a non-empty `roles` array. The requested roles may not grant permissions the current actor does not have. Only a SUPER_ADMIN can assign SUPER_ADMIN. There is intentionally no public admin bootstrap endpoint; provision the initial administrator through a controlled operator process.

Roles and permissions are loaded from the database for each bearer-token request. Each user-role assignment also captures the permissions granted by that role at assignment time. Effective permissions are limited to the intersection of that snapshot and the role's currently enabled permissions: removals take effect immediately, while permissions added to a role later do not expand existing users' access. A role must be reassigned to grant newly added permissions.

## Configuration

Generate a signing key locally:

```bash
openssl rand -base64 32
```

Set the output as `JWT_SECRET` in the process environment or ignored `.env`. The application requires this value and validates a minimum 256-bit signing key. Never commit it, expose it to frontend builds, or reuse the test-only value from `src/test/resources/application.yml`.

| Variable | Default | Description |
| --- | --- | --- |
| `JWT_SECRET` | Required | Base64 or UTF-8 HMAC key; at least 256 bits |
| `JWT_ACCESS_EXPIRATION` | `900000` | Access lifetime in milliseconds |
| `JWT_REFRESH_EXPIRATION` | `604800000` | Refresh lifetime in milliseconds |
| `JWT_ISSUER` | `designer-costume-api` | Token issuer |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:3000` | Exact comma-separated browser origins; no wildcard credentials |
| `AUTH_COOKIE_SECURE` | `true` | Set false only for local HTTP development |
| `PASSWORD_RESET_DELIVERY` | `smtp` | `smtp` or explicit local `development` mode |
| `PASSWORD_RESET_EXPIRATION` | `900000` | Reset-token lifetime in milliseconds |
| `PASSWORD_RESET_BASE_URL` | `http://localhost:3000/reset-password` | Frontend destination used in reset emails |
| `SMTP_HOST`, `SMTP_PORT` | `localhost`, `25` | SMTP server |
| `SMTP_USERNAME`, `SMTP_PASSWORD` | empty | SMTP credentials when required |
| `SMTP_AUTH`, `SMTP_STARTTLS` | `false` | SMTP authentication and STARTTLS switches |
| `SMTP_FROM` | `no-reply@designer-costume.local` | Sender address |

Use HTTPS, `AUTH_COOKIE_SECURE=true`, a strong unique key, explicit CORS origins, and SMTP delivery in production.

## Database changes

Flyway migrations V10 and V11 add refresh-session/password-reset token tables and grant CUSTOMER only catalog read permissions. V12 snapshots permissions for existing user-role assignments to prevent future role edits from silently expanding existing users' access. Existing migrations V1–V11 are unchanged. Hibernate remains configured with `ddl-auto: validate`.

## Verification

From `services/backend`, run:

```bash
mvn test
```

The tests use an isolated embedded PostgreSQL 16 database and verify Flyway migrations, schema validation, registration, authentication, refresh rotation/reuse revocation and concurrency, password-reset token/session behavior, cookie CSRF protection, CORS origin restrictions, role assignment limits and permission snapshots, inactive-user access-token rejection, and existing catalog behavior.
