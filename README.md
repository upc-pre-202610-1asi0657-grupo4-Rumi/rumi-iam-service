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

Registers administrators (US01), signs users in (US02), recovers forgotten passwords with single-use links (US03),
keeps the Firebase tokens of the users' devices so that Notification can push earthquake alerts (US14) and links users
to their buildings from the events of Building Management. Sign-in issues
the HS256 JWT that
[`rumi-api-gateway`](https://github.com/upc-pre-202610-1asi0657-grupo4-Rumi/rumi-api-gateway) validates locally on every
request.

## Origin

New service. It has no code in the modular monolith
[`rumi-backend`](https://github.com/upc-pre-202610-1asi0657-grupo4-Rumi/rumi-backend); it is one of the
bounded contexts of the target architecture defined when the monolith was decomposed.

## Endpoints

`/api/v1/authentication/**` is public behind the gateway (no token). `/api/v1/device-tokens/**` and
`/api/v1/users/**` need a valid token: the gateway validates it and passes the user id in `X-User-Id`.

| Verb | Path | Description | Request | Response | User story | Status |
|---|---|---|---|---|---|---|
| POST | `/api/v1/authentication/sign-up` | Register an administrator | `SignUpRequest` | `201` `UserResponse` | US01 | implemented |
| POST | `/api/v1/authentication/sign-in` | Sign in and get a token | `SignInRequest` | `200` `AuthenticationResponse` | US02 | implemented |
| POST | `/api/v1/authentication/forgot-password` | Request a password reset link | `ForgotPasswordRequest` | `202`, empty body | US03 | implemented |
| POST | `/api/v1/authentication/reset-password` | Set a new password with a reset link | `ResetPasswordRequest` | `204`, empty body | US03 | implemented |
| POST | `/api/v1/device-tokens` | Register the device of the signed-in user | `DeviceTokenRequest` | `201` / `200` `DeviceTokenResponse` | US14 | implemented |
| POST | `/api/v1/users/me/token` | Reissue the token of the signed-in user with its current buildings | none (`Authorization: Bearer`) | `200` `AuthenticationResponse` | US04 | implemented |
| GET | `/api/v1/authentication/health` | Check that the service is running | none | `200` `HealthResponse` | none | skeleton |

Implemented functional endpoints: 6. Skeleton endpoints: 1.

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

- Every new user is an `ADMINISTRATOR` with no buildings. A building is added when the administrator registers it
  (see [Building membership](#building-membership)). Residents will join through invitation codes later.
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
  "expiresAt": "2026-10-29T15:15:00Z",
  "buildingIds": []
}
```

An unknown email and a wrong password both answer `401` with the same detail, `Invalid email or password.`
`buildingIds` is the same list as the `buildingIds` claim of the token.

### Building membership

IAM fills `user_building_ids`, and so the `buildingIds` claim, from the events of rumi-building-service:

| Event | Queue (dead letters in `<queue>.dlq`) | Effect | Status |
|---|---|---|---|
| `BuildingRegistered` | `rumi.iam.building-registered`, bound to `rumi.building-management.events` with `building-management.building.registered` | The administrator who registered the building is linked to it | implemented |
| `ResidentJoinedBuilding` | `rumi.iam.resident-joined-building` (planned) | The resident who accepted an invitation is linked to the building | contract only, pending |

- Building publishes through a transactional outbox, so an event can arrive more than once. IAM applies each
  `eventId` once (`processed_events`) and the key of `user_building_ids` avoids a duplicate link.
- An event for an unknown user or for a user who is not an administrator, or a malformed message, goes to the dead
  letter queue. Other failures are retried 3 times (1 s, 2 s) before going there.
- With the `dev` profile there is no RabbitMQ: the listener does not start, and the demo users come already linked.

### Token reissue

The building appears in the token only after IAM has processed the event, a moment after `POST /api/v1/buildings`
answers. **The frontend calls `POST /api/v1/users/me/token` after registering a building (and, once it exists,
after accepting an invitation), and retries briefly** (for example every 500 ms, up to 5 s) until `buildingIds`
contains the new building.

```
POST /api/v1/users/me/token
Authorization: Bearer <current token>
```

```json
200 OK
{
  "userId": "7d3c5b1e-2f4a-4c8e-9b6d-1a2b3c4d5e6f",
  "email": "ana.torres@rumi.pe",
  "role": "ADMINISTRATOR",
  "token": "<new jwt>",
  "tokenType": "Bearer",
  "expiresAt": "2026-10-29T16:15:00Z",
  "buildingIds": ["7a9b3c1d-2e4f-4b6a-8c0d-1e2f3a4b5c6d"]
}
```

- IAM validates the bearer token itself (signature and expiration) and checks that it belongs to `X-User-Id`.
- The new token keeps the `auth_time` of the password sign-in. **12 hours after the sign-in**
  (`TOKEN_REISSUE_MAX_SESSION_AGE`) the answer is `401` (`The session started more than 12 hours ago; sign in
  again`), so a stolen token cannot be renewed forever. Tokens without `auth_time` (issued before this change)
  cannot be reissued.
- Revoking tokens before they expire (`tokenVersion`) is not implemented yet.

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
| `401` | Wrong credentials on sign-in; no valid `X-User-Id` on device-tokens or users/me/token; invalid bearer token or session older than 12 hours on users/me/token |
| `409` | Email already registered on sign-up |

## Token contract with the gateway

HS256, signed with `JWT_SECRET` (the same value the gateway uses). Claims:

| Claim | Value |
|---|---|
| `sub` | User id (UUID) |
| `role` | `ADMINISTRATOR` or `RESIDENT` |
| `buildingIds` | List of building UUIDs; always present, `[]` for a new administrator |
| `auth_time` | When the user signed in with a password (epoch seconds, as in OpenID Connect); kept by a reissue |
| `iat`, `exp` | Issue and expiration time; `exp` = `iat` + `JWT_EXPIRATION` (15 minutes by default) |

There is no refresh token: before the token expires, the client can get a new one with
`POST /api/v1/users/me/token`, up to 12 hours after the password sign-in. After that it must sign in again.

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

The `dev` profile creates two demo users, already linked to the sample building of the other services
(`7a9b3c1d-2e4f-4b6a-8c0d-1e2f3a4b5c6d`). Their password is a public demo value, valid only on a local H2 database;
`DevDataSeederTest` checks that they are never created outside the `dev` profile.

| User | Id | Email | Password | Role |
|---|---|---|---|---|
| Administradora Demo | `c1d2e3f4-a5b6-4c7d-8e9f-0a1b2c3d4e5f` | `admin.demo@rumi.dev` | `rumi-demo-2026` | `ADMINISTRATOR` |
| Residente Demo | `d4e5f6a7-b8c9-4d0e-9f1a-2b3c4d5e6f70` | `residente.demo@rumi.dev` | `rumi-demo-2026` | `RESIDENT` |

With PostgreSQL and RabbitMQ (default profile): create `iamdb` and apply [`docs/schema.sql`](docs/schema.sql) first.

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
| `TOKEN_REISSUE_MAX_SESSION_AGE` | `PT12H` (`rumi.security.token-reissue.max-session-age`): reissue limit after the sign-in |
| `RABBITMQ_HOST` / `RABBITMQ_PORT` | `localhost` / `5672` |
| `RABBITMQ_USERNAME` / `RABBITMQ_PASSWORD` | `guest` / `guest` |

## Test

```sh
./mvnw test
```

Runs the unit tests, the MockMvc and JPA slice tests, the gateway token contract test and the Cucumber scenarios
of [`src/test/resources/features`](src/test/resources/features) (tagged `@US01`, `@US02`, `@US03`, `@US04` and `@US14`)
with a fake test-only secret.

**Docker must be running.** The JPA tests, the Cucumber scenarios and the context test share one PostgreSQL 16
container (Testcontainers), created from [`docs/schema.sql`](docs/schema.sql) with Hibernate in `validate` mode, so the
build fails if the schema and the JPA entities disagree. `src/test/resources/docker-java.properties` pins Docker API
1.44, which Docker Engine 25 and later accept (Engine 29 rejects the older default of the client).

The tests do not need the RabbitMQ of Docker Compose: the listener is off in the `test` profile, and
`BuildingRegisteredSubscriberIntegrationTest` starts its own RabbitMQ container.

## Structure

```
com.rumi.iam
├── domain
│   ├── model          User (aggregate root), PasswordResetToken, DeviceToken, DevicePlatform, Role,
│   │                  EmailAddress, PasswordPolicy
│   ├── repository     UserRepository, PasswordResetTokenRepository, DeviceTokenRepository,
│   │                  ProcessedEventRepository
│   └── service        PasswordHasher, TokenIssuer, IssuedToken, AccessTokenReader, ResetTokenGenerator,
│                      PasswordResetNotifier (Anti-Corruption Layer towards mail)
├── application        AuthenticationApplicationService (sign-up, sign-in, password recovery),
│                      DeviceTokenApplicationService, BuildingMembershipApplicationService,
│                      TokenReissueApplicationService
└── infrastructure     OpenAPI configuration
    ├── messaging      RabbitMQ queue, dead letter queue and BuildingRegistered listener
    ├── persistence    JPA entities and repositories
    ├── security       BCrypt hasher, JWT issuer and reader, SHA-256 reset token generator, properties
    ├── seed           demo users of the dev profile
    ├── notification   LoggingPasswordResetNotifier (development stand-in)
    └── web            authentication, current-user, device-token and health endpoints, X-User-Id resolver,
                       DTOs, problem details
```

## Follow-ups

- Link residents when they accept an invitation (`ResidentJoinedBuilding`, pending).
- Add `GET /api/v1/users/{id}`, then return a `Location` header from sign-up.
- A mail adapter for `PasswordResetNotifier` once Notification can send e-mail.
- `DELETE /api/v1/device-tokens/{id}` to unregister a device at sign-out.
- Token revocation with a `tokenVersion` per user, also for tokens issued before a password reset.
