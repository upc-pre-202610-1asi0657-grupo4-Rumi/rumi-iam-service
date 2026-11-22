# rumi-iam-service

Identity & Access Management service of **Rumi**, the structural monitoring platform by Kuntur Labs.

| | |
|---|---|
| Bounded context | Identity & Access Management |
| Port | `8084` |
| Gateway routes | `/api/v1/authentication/**`, `/api/v1/users/**`, `/api/v1/device-tokens/**` |
| Base package | `com.rumi.iam` |
| Database | PostgreSQL `iamdb` (in-memory H2 with the `dev` profile) |

## Purpose

Registers administrators (US01), signs users in (US02), recovers forgotten passwords with single-use links (US03) and
keeps the Firebase tokens of the users' devices so that Notification can push earthquake alerts (US14). Sign-in issues
the HS256 JWT that
[`rumi-api-gateway`](https://github.com/upc-pre-202610-1asi0657-grupo4-Rumi/rumi-api-gateway) validates locally on every
request.

## Origin

New service. It has no code in the modular monolith
[`rumi-backend`](https://github.com/upc-pre-202610-1asi0657-grupo4-Rumi/rumi-backend); it is one of the
bounded contexts of the target architecture defined when the monolith was decomposed.

## Endpoints

`/api/v1/authentication/**` is public behind the gateway (no token). `/api/v1/device-tokens/**` needs a valid token:
the gateway validates it and passes the user id in `X-User-Id`.

| Verb | Path | Description | Request | Response | User story | Status |
|---|---|---|---|---|---|---|
| POST | `/api/v1/authentication/sign-up` | Register an administrator | `SignUpRequest` | `201` `UserResponse` | US01 | implemented |
| POST | `/api/v1/authentication/sign-in` | Sign in and get a token | `SignInRequest` | `200` `AuthenticationResponse` | US02 | implemented |
| POST | `/api/v1/authentication/forgot-password` | Request a password reset link | `ForgotPasswordRequest` | `202`, empty body | US03 | implemented |
| POST | `/api/v1/authentication/reset-password` | Set a new password with a reset link | `ResetPasswordRequest` | `204`, empty body | US03 | implemented |
| POST | `/api/v1/device-tokens` | Register the device of the signed-in user | `DeviceTokenRequest` | `201` / `200` `DeviceTokenResponse` | US14 | implemented |
| GET | `/api/v1/authentication/health` | Check that the service is running | none | `200` `HealthResponse` | none | skeleton |

Implemented functional endpoints: 5. Skeleton endpoints: 1.

### Sign-up (US01)

```json
POST /api/v1/authentication/sign-up
{
  "email": "ana.torres@rumi.pe",
  "password": "Sismo-Seguro-2026",
  "fullName": "Ana Torres"
}
```

```json
201 Created
{
  "id": "7d3c5b1e-2f4a-4c8e-9b6d-1a2b3c4d5e6f",
  "email": "ana.torres@rumi.pe",
  "fullName": "Ana Torres",
  "role": "ADMINISTRATOR"
}
```

- Every new user is an `ADMINISTRATOR` with no buildings. Residents will join through invitation codes later.
- The email is stored trimmed and in lowercase, and must be unique: `409 Conflict` otherwise.
- The password must have **at least 8 characters** (code points) and **at most 72 bytes** in UTF-8, the part BCrypt
  uses. It is stored only as a BCrypt hash.
- No `Location` header yet: `GET /api/v1/users/{id}` does not exist.

### Sign-in (US02)

```json
POST /api/v1/authentication/sign-in
{
  "email": "ana.torres@rumi.pe",
  "password": "Sismo-Seguro-2026"
}
```

```json
200 OK
{
  "userId": "7d3c5b1e-2f4a-4c8e-9b6d-1a2b3c4d5e6f",
  "email": "ana.torres@rumi.pe",
  "role": "ADMINISTRATOR",
  "token": "<jwt>",
  "tokenType": "Bearer",
  "expiresAt": "2026-10-29T15:15:00Z"
}
```

An unknown email and a wrong password both answer `401` with the same detail, `Invalid email or password.`

### Password recovery (US03)

```json
POST /api/v1/authentication/forgot-password
{
  "email": "ana.torres@rumi.pe"
}
```

```
202 Accepted   (empty body)
```

```json
POST /api/v1/authentication/reset-password
{
  "token": "<value of the token parameter of the link>",
  "newPassword": "Nueva-Clave-Sismo-2026"
}
```

```
204 No Content   (empty body)
```

- `forgot-password` always answers `202`, whether the email is registered or not, so it does not reveal which emails
  exist. A malformed email gives `400`.
- The link is `${PASSWORD_RESET_BASE_URL}?token=<token>`. The token is 32 random bytes (`SecureRandom`) in URL-safe
  Base64; only its SHA-256 is stored. The link works **once** and for **30 minutes** (`PASSWORD_RESET_TTL`).
- Asking again invalidates the earlier unused links. A successful reset invalidates every other unused link too.
- The new password follows the same rule as sign-up. A weak password gives `400` and does not use up the link.
- An unknown, expired or already used link gives `400` with the same detail, `Invalid or expired token.`
- Access tokens issued before the reset stay valid until they expire (15 minutes): there is no revocation list yet.

> **Development stand-in: no e-mail is sent.** Rumi has no mail sender yet, so the only adapter of the
> `PasswordResetNotifier` port, `LoggingPasswordResetNotifier`, writes the reset link to the service log (a `WARN`
> line that says so). That line is the only place where the link appears. Replace it with a mail adapter before real
> users depend on password recovery.

### Device tokens (US14)

Called through the gateway with `Authorization: Bearer <token>`; the gateway adds `X-User-Id`.

```json
POST /api/v1/device-tokens
{
  "token": "<Firebase Cloud Messaging registration token>",
  "platform": "ANDROID"
}
```

```json
201 Created
{
  "id": "5c1d7e9a-3b2f-4d6e-8a1c-9f0e2d4b6a8c",
  "platform": "ANDROID",
  "registeredAt": "2026-10-29T15:00:00Z"
}
```

- `platform` is `ANDROID`, `IOS` or `WEB`. The token has at most 1024 characters and is never returned or logged.
- Registering a token that is already known answers `200` with the same body. If it belonged to another user (the
  phone changed account), it moves to the signed-in user. A user can have many devices.
- A missing or malformed `X-User-Id` header gives `401`.

> **IAM trusts the gateway headers.** The gateway removes any `X-User-Id` sent by a client and sets it from a valid
> token, so in a real deployment IAM must only be reachable through the gateway, never exposed directly.

### Errors

Every error is an RFC 7807 problem detail (`application/problem+json`). Validation errors list the fields in `errors`:

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "Invalid request content.",
  "instance": "/api/v1/authentication/sign-up",
  "errors": {
    "password": "Password must have at least 8 characters and at most 72 bytes"
  }
}
```

| Status | When |
|---|---|
| `400` | Invalid data (`errors` map), malformed body, or `Invalid or expired token.` on reset-password |
| `401` | Wrong credentials on sign-in, or no valid `X-User-Id` on device-tokens |
| `409` | Email already registered on sign-up |

## Token contract with the gateway

HS256, signed with `JWT_SECRET` (the same value the gateway uses). Claims:

| Claim | Value |
|---|---|
| `sub` | User id (UUID) |
| `role` | `ADMINISTRATOR` or `RESIDENT` |
| `buildingIds` | List of building UUIDs; always present, `[]` for a new administrator |
| `iat`, `exp` | Issue and expiration time; `exp` = `iat` + `JWT_EXPIRATION` (15 minutes by default) |

There is no refresh token yet: when the token expires, the client must sign in again to get a new one.

`GatewayTokenCompatibilityTest` decodes the issued tokens exactly as the gateway does, so a change that breaks the
contract fails the build.

## API documentation

- Swagger UI: <http://localhost:8084/swagger-ui.html> (every response code has an example)
- OpenAPI spec: <http://localhost:8084/v3/api-docs>
- Exported spec: [`docs/openapi.json`](docs/openapi.json)
- Database schema: [`docs/schema.sql`](docs/schema.sql)

## Run

Requirements: JDK 21 (the Maven wrapper is included; use `mvnw.cmd` on Windows).

Without any infrastructure, on an in-memory H2 database:

```sh
JWT_SECRET=<at least 32 bytes, same as the gateway> ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

With PostgreSQL (default profile): create `iamdb` and apply [`docs/schema.sql`](docs/schema.sql) first.

| Variable | Default |
|---|---|
| `SERVER_PORT` | `8084` |
| `JWT_SECRET` | none: required, at least 32 bytes; the service does not start without it |
| `JWT_EXPIRATION` | `PT15M` (ISO-8601 duration) |
| `IAM_DB_URL` | `jdbc:postgresql://localhost:5432/iamdb` |
| `IAM_DB_USERNAME` | `postgres` |
| `IAM_DB_PASSWORD` | `postgres` |
| `PASSWORD_RESET_BASE_URL` | `http://localhost:4200/reset-password` (`rumi.password-reset.base-url`): page that opens reset links |
| `PASSWORD_RESET_TTL` | `PT30M` (`rumi.password-reset.ttl`): how long a reset link works |

## Test

```sh
./mvnw test
```

Runs the unit tests, the MockMvc and JPA slice tests, the gateway token contract test and the Cucumber scenarios
of [`src/test/resources/features`](src/test/resources/features) (tagged `@US01`, `@US02`, `@US03` and `@US14`) with a
fake test-only secret.

**Docker must be running.** The JPA tests, the Cucumber scenarios and the context test share one PostgreSQL 16
container (Testcontainers), created from [`docs/schema.sql`](docs/schema.sql) with Hibernate in `validate` mode, so the
build fails if the schema and the JPA entities disagree. `src/test/resources/docker-java.properties` pins Docker API
1.44, which Docker Engine 25 and later accept (Engine 29 rejects the older default of the client).

## Structure

```
com.rumi.iam
├── domain
│   ├── model          User (aggregate root), PasswordResetToken, DeviceToken, DevicePlatform, Role,
│   │                  EmailAddress, PasswordPolicy
│   ├── repository     UserRepository, PasswordResetTokenRepository, DeviceTokenRepository
│   └── service        PasswordHasher, TokenIssuer, IssuedToken, ResetTokenGenerator,
│                      PasswordResetNotifier (Anti-Corruption Layer towards mail)
├── application        AuthenticationApplicationService (sign-up, sign-in, password recovery),
│                      DeviceTokenApplicationService
└── infrastructure     OpenAPI configuration
    ├── persistence    JPA entities and repositories
    ├── security       BCrypt hasher, JWT issuer, SHA-256 reset token generator, properties
    ├── notification   LoggingPasswordResetNotifier (development stand-in)
    └── web            authentication, device-token and health endpoints, X-User-Id resolver, DTOs,
                       problem details
```

## Follow-ups

- Fill `user_building_ids` when an administrator registers a building or a resident uses an invitation.
- Add `GET /api/v1/users/{id}`, then return a `Location` header from sign-up.
- A mail adapter for `PasswordResetNotifier` once Notification can send e-mail.
- `DELETE /api/v1/device-tokens/{id}` to unregister a device at sign-out.
- A revocation list for access tokens issued before a password reset.
