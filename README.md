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

Registers administrators (US01) and signs users in (US02). Sign-in issues the HS256 JWT that
[`rumi-api-gateway`](https://github.com/upc-pre-202610-1asi0657-grupo4-Rumi/rumi-api-gateway) validates locally on every
request. Password recovery (US03) and device tokens for push notifications are planned for later sprints.

## Origin

New service. It has no code in the modular monolith
[`rumi-backend`](https://github.com/upc-pre-202610-1asi0657-grupo4-Rumi/rumi-backend); it is one of the
bounded contexts of the target architecture defined when the monolith was decomposed.

## Endpoints

All of them are public behind the gateway: `/api/v1/authentication/**` needs no token.

| Verb | Path | Description | Request | Response | User story | Status |
|---|---|---|---|---|---|---|
| POST | `/api/v1/authentication/sign-up` | Register an administrator | `SignUpRequest` | `201` `UserResponse` | US01 | implemented |
| POST | `/api/v1/authentication/sign-in` | Sign in and get a token | `SignInRequest` | `200` `AuthenticationResponse` | US02 | implemented |
| GET | `/api/v1/authentication/health` | Check that the service is running | none | `200` `HealthResponse` | none | skeleton |

Implemented functional endpoints: 2. Skeleton endpoints: 1.

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
  "expiresAt": "2026-10-29T16:00:00Z"
}
```

An unknown email and a wrong password both answer `401` with the same detail, `Invalid email or password.`

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

## Token contract with the gateway

HS256, signed with `JWT_SECRET` (the same value the gateway uses). Claims:

| Claim | Value |
|---|---|
| `sub` | User id (UUID) |
| `role` | `ADMINISTRATOR` or `RESIDENT` |
| `buildingIds` | List of building UUIDs; always present, `[]` for a new administrator |
| `iat`, `exp` | Issue and expiration time; `exp` = `iat` + `JWT_EXPIRATION` |

`GatewayTokenCompatibilityTest` decodes the issued tokens exactly as the gateway does, so a change that breaks the
contract fails the build.

## API documentation

- Swagger UI: <http://localhost:8084/swagger-ui.html>
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
| `JWT_EXPIRATION` | `PT1H` (ISO-8601 duration) |
| `IAM_DB_URL` | `jdbc:postgresql://localhost:5432/iamdb` |
| `IAM_DB_USERNAME` | `postgres` |
| `IAM_DB_PASSWORD` | `postgres` |

## Test

```sh
./mvnw test
```

Runs the unit tests, the MockMvc and JPA slice tests, the gateway token contract test and the Cucumber scenarios
of [`src/test/resources/features`](src/test/resources/features) (tagged `@US01` and `@US02`). Everything runs on
in-memory H2 with a fake test-only secret; no Docker is needed.

## Structure

```
com.rumi.iam
├── domain
│   ├── model          User, Role, EmailAddress, PasswordPolicy
│   ├── repository     UserRepository
│   └── service        PasswordHasher, TokenIssuer, IssuedToken
├── application        AuthenticationApplicationService (sign-up, sign-in)
└── infrastructure     OpenAPI configuration
    ├── persistence    JPA entity and repository
    ├── security       BCrypt hasher, JWT issuer, JWT_SECRET properties
    └── web            authentication and health endpoints, DTOs, problem details
```

## Follow-ups

- Fill `user_building_ids` when an administrator registers a building or a resident uses an invitation.
- Add `GET /api/v1/users/{id}`, then return a `Location` header from sign-up.
- Password recovery (US03) and device tokens.
