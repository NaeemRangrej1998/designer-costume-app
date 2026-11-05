# Architecture

## Service boundaries

```text
Web client (Next.js) ──┐
                       ├── HTTP/JSON ──> Spring Boot API ──> PostgreSQL
Mobile client (Expo) ──┘                       │
                                               └── future internal API call ──> FastAPI AI service
```

The web and mobile applications are independent clients, but they share one Spring Boot API. The backend is responsible for business rules, validation, and persistence. PostgreSQL is not accessed directly by either client.

The Python AI service is independently deployable and owns future model-runtime concerns. In a later phase, the backend should call it over a private service-to-service boundary; clients should not receive direct access to model infrastructure or credentials. No model is included in the current phase.

## Backend layering

The backend uses conventional Spring layers:

- `controller`: HTTP request/response handling
- `service`: application use cases and business logic
- `repository`: persistence interfaces
- `entity`: persistence models
- `dto`: API data contracts, including common envelopes and page metadata
- `mapper`: reserved for entity/DTO conversions where projection queries are not suitable
- `config`, `exception`, and `util`: cross-cutting configuration and helpers

The health and read-only catalog controllers delegate to services. Catalog list/detail and variant repositories use DTO projections to avoid entity serialization and unnecessary relation loading.

Authentication is stateless: the backend validates short-lived JWT access tokens and resolves the user's current permissions from PostgreSQL on each request. Hashed refresh-token records support rotation, family revocation, and reuse detection. Web clients receive refresh credentials only in HttpOnly cookies and use double-submit CSRF validation for cookie-based refresh/logout; mobile clients receive refresh tokens in JSON for platform secure storage. Password reset tokens are also stored only as hashes.

## Data and migrations

Spring Data JPA/Hibernate provides persistence integration, and Flyway applies the versioned schema migrations. Hibernate validates the migrated schema at startup (`ddl-auto: validate`) and does not create or alter tables. The database is PostgreSQL 16 and uses a named Docker volume for local persistence. Application credentials are supplied through environment variables, not source-controlled configuration. See [database-design.md](./database-design.md) for table relationships and constraints.

## Networking

- Host-run web app: `localhost:3000`
- Host-run backend: `localhost:8080`
- Host-run AI service: `localhost:8001` by default
- PostgreSQL: `localhost:5432`

Within Docker Compose, the database hostname is `db`. The backend currently runs directly on the host in the documented workflow, so its database host is `localhost`. Compose is currently responsible for PostgreSQL only.

## Health checks

- `GET /api/v1/health`: application API liveness contract
- `GET /actuator/health`: Spring Boot operational health
- `GET /health`: AI service liveness contract
- Compose `pg_isready`: PostgreSQL container readiness

The API liveness endpoint intentionally does not imply database readiness; Actuator reports the backend's database health separately.
