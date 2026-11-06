# Designer Costume Studio

A monorepo for a designer costume boutique's virtual try-on platform. It contains independently runnable web, mobile, backend, and AI-service projects, plus local PostgreSQL infrastructure. Phase 1 established the application foundations; Phase 2 added the normalized database model, JPA entities, repositories, Flyway migrations, and seed access-control data. Phase 3 added catalog read APIs. Phase 4 adds customer authentication, JWT access tokens, rotating persisted refresh sessions, password recovery, and permission-guarded administration APIs. Phase 5 adds the admin dashboard and permission-checked catalog, inventory, user, inquiry, and role management. Phase 8 adds local image storage, costume galleries, and private customer photo libraries; AI model integration remains future work.

## Technology stack

| Area | Foundation |
| --- | --- |
| Web | Next.js App Router, React, JavaScript, Tailwind CSS, ESLint, Axios |
| Mobile | Expo, React Native, JavaScript, Axios |
| Backend | Java 21, Spring Boot 3.5, Maven, Spring Web, JPA, Validation, Spring Security, Actuator, Lombok |
| Database | PostgreSQL 16, Hibernate, Flyway |
| AI service | Python 3.11+, FastAPI, Uvicorn |
| Infrastructure | Docker Compose |

TypeScript is not used. Cloud image storage, persisted favorites, customer inquiry submission, AI try-on processing, and model integration remain out of scope.

## Architecture

The web and mobile applications are separate clients and will use the shared Spring Boot API. The backend owns business rules and PostgreSQL persistence. The AI service is a separate Python service that the backend can call for future virtual try-on work; it does not load or train a model in Phase 1.

See [docs/architecture.md](docs/architecture.md) for service communication, [docs/database-design.md](docs/database-design.md) for the schema, constraints, delete behavior, and ER diagram, [docs/api-documentation.md](docs/api-documentation.md) for REST endpoint details, [docs/authentication-and-security.md](docs/authentication-and-security.md) for auth/security behavior, and [docs/admin-panel.md](docs/admin-panel.md) for Phase 5 routes and backend API gaps.

## Project structure

```text
apps/
  web/                 Next.js application
  mobile/              Expo / React Native application
services/
  backend/             Spring Boot API
  ai/                  FastAPI service foundation
infrastructure/
  docker-compose.yml   PostgreSQL 16 and backend with persistent uploads
docs/
  project-overview.md
  architecture.md
  development-setup.md
.env.example
README.md
```

Each application has its own manifest and can be run separately. PostgreSQL is optional for the standalone welcome screens and AI health check; the backend needs PostgreSQL for its JPA datasource.

## Prerequisites

- Node.js 20.9+ and npm
- Java 21 JDK and Maven 3.6.3+
- Python 3.11+ and pip
- Docker Engine with Docker Compose v2
- Git

The development environment used to create this foundation was checked before implementation. It has Node.js 24 and Maven, but currently has Java 17, Python 3.10, and no Docker CLI. Java 21, Python 3.11+, and Docker are needed for the corresponding full runtime checks. Backend integration tests use an isolated embedded PostgreSQL 16 database and do not require Docker.

## Initial setup

From the repository root:

```bash
cp .env.example .env
```

Change `DB_PASSWORD` and set `JWT_SECRET` in `.env` before running the backend. Generate the JWT secret with `openssl rand -base64 32`. The root `.env` is ignored by Git. Each service also has an `.env.example` with its service-specific settings. Spring Boot reads process environment variables; it does not load a dotenv file by itself. Export the database/auth variables in the shell or use a dotenv loader of your choice when running the backend.

To configure the web client locally, copy `apps/web/.env.example` to `apps/web/.env.local`. The shared API base URL should point to the running backend.

## Start PostgreSQL

Docker Compose reads the root `.env` automatically:

```bash
docker compose -f infrastructure/docker-compose.yml up -d db
docker compose -f infrastructure/docker-compose.yml ps
```

PostgreSQL is exposed on `localhost:5432` and persists data in the named `postgres_data` volume. To stop the database without removing its data:

```bash
docker compose -f infrastructure/docker-compose.yml down
```

To remove the database volume as well (destructive to local database data):

```bash
docker compose -f infrastructure/docker-compose.yml down -v
```

## Run the backend

Provide database settings and a strong `JWT_SECRET` in the process environment. Generate a secret with `openssl rand -base64 32`; the application refuses to start without one. From the backend directory, export the root `.env` and start Spring Boot:

```bash
cd services/backend
set -a
source ../../.env
set +a
mvn spring-boot:run
```

To compile and package:

```bash
cd services/backend
mvn clean verify
```

Alternatively, run the containerized backend (which starts PostgreSQL as a dependency) from the root after setting `DB_PASSWORD` and `JWT_SECRET` in `.env`:

```bash
docker compose -f infrastructure/docker-compose.yml up -d --build backend
```

The health endpoint is `GET http://localhost:8080/api/v1/health`; active category, costume, image, and available variant GET endpoints are public and provide catalog data only. Customer photo endpoints require the authenticated owner. Authentication endpoints are under `/api/v1/auth`; admin APIs still require bearer authentication and permissions. Spring Boot Actuator health is at `GET http://localhost:8080/actuator/health`. Swagger UI is at `http://localhost:8080/swagger-ui/index.html` and OpenAPI JSON is at `/v3/api-docs`. Flyway applies migrations V1-V14 at backend startup, and Hibernate validates the resulting schema. Local uploads use `STORAGE_ROOT` and `MAX_FILE_SIZE`; Compose mounts the named `uploads_data` volume. Configure SMTP for production password reset email; development delivery is for local testing only. Enable development SQL statement logging with `mvn spring-boot:run -Dspring-boot.run.profiles=dev`.

## Run the web application

```bash
cd apps/web
npm install
npm run dev
```

Open `http://localhost:3000`. To create a production build, run `npm run build`; to serve it locally, run `npm start`.

The customer storefront is at `/`, with customer registration at `/register` and login at `/login`. The admin panel is at `/admin`. Configure `CORS_ALLOWED_ORIGINS` to the exact web origin and `AUTH_COOKIE_SECURE=false` only for local HTTP. Optional shop contact details can be configured with `NEXT_PUBLIC_SHOP_ADDRESS`, `NEXT_PUBLIC_SHOP_PHONE`, and `NEXT_PUBLIC_SHOP_EMAIL`. See [docs/customer-storefront.md](docs/customer-storefront.md) for implemented pages and backend limitations.

## Run the mobile application

```bash
cd apps/mobile
npm install
npm start
```

Copy `apps/mobile/.env.example` to `apps/mobile/.env` and set `EXPO_PUBLIC_API_BASE_URL` to a host reachable from your target device. Android emulators use `http://10.0.2.2:8080/api/v1`; physical devices need the development computer's LAN IP. Use the Expo terminal prompt to launch a simulator or scan the QR code with Expo Go. See [apps/mobile/README.md](apps/mobile/README.md) for auth, API, and platform setup details.

## Run the AI service

```bash
cd services/ai
cp .env.example .env
python3 -m venv .venv
source .venv/bin/activate
python -m pip install -r requirements.txt
set -a
source .env
set +a
uvicorn app.main:app --reload --host 0.0.0.0 --port "${AI_SERVICE_PORT:-8001}"
```

Check `GET http://localhost:8001/health`. Set `AI_SERVICE_PORT` to choose another port.

## Development URLs

| Service | URL |
| --- | --- |
| Web (development) | http://localhost:3000 |
| Backend API health | http://localhost:8080/api/v1/health |
| Spring Actuator health | http://localhost:8080/actuator/health |
| AI service health | http://localhost:8001/health |
| PostgreSQL | localhost:5432 |

## Environment variables

| Variable | Used by | Default / example |
| --- | --- | --- |
| `DB_HOST` | Backend / Compose | `localhost` for host-run backend; `db` inside Compose |
| `DB_PORT` | Backend / Compose | `5432` |
| `DB_NAME` | Backend / Compose | `costume_db` |
| `DB_USERNAME` | Backend / Compose | `costume_user` |
| `DB_PASSWORD` | Backend / Compose | Required; set a local value in ignored `.env` |
| `JWT_SECRET` | Backend | Required; generate with `openssl rand -base64 32` |
| `JWT_ACCESS_EXPIRATION` | Backend | `900000` ms (15 minutes) |
| `JWT_REFRESH_EXPIRATION` | Backend | `604800000` ms (7 days) |
| `STORAGE_ROOT` | Backend | `./storage` for host-run; `/app/storage` in Compose |
| `MAX_FILE_SIZE` | Backend | `10485760` bytes (10 MiB) |
| `MAX_REQUEST_SIZE` | Backend | `11534336` bytes |
| `CORS_ALLOWED_ORIGINS` | Backend | Comma-separated exact browser origins |
| `AUTH_COOKIE_SECURE` | Backend | `true` in production; `false` only for local HTTP |
| `PASSWORD_RESET_DELIVERY` | Backend | `development` for local token response, `smtp` for email |
| `PASSWORD_RESET_EXPIRATION` | Backend | `900000` ms (15 minutes) |
| `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`, `SMTP_PASSWORD` | Backend | SMTP connection for production reset email |
| `NEXT_PUBLIC_API_BASE_URL` | Web | `http://localhost:8080/api/v1` |
| `EXPO_PUBLIC_API_BASE_URL` | Mobile | Android emulator default: `http://10.0.2.2:8080/api/v1`; set a LAN URL for physical devices |
| `EXPO_PUBLIC_SHOP_ADDRESS`, `EXPO_PUBLIC_SHOP_PHONE`, `EXPO_PUBLIC_SHOP_EMAIL` | Mobile | Optional public boutique contact details |
| `AI_SERVICE_PORT` | AI service | `8001` |

The web API URL configures the admin panel Axios client. Set the backend's `CORS_ALLOWED_ORIGINS` to the exact frontend origin and use `AUTH_COOKIE_SECURE=false` only for local HTTP development. Never put production credentials in tracked files.

## Phase status

The web application includes the customer storefront and permission-aware admin workspace. The mobile application includes a customer storefront, account flows, and local try-on preparation with an opt-in private photo library. Favorites are not persisted, customer inquiry submission is unavailable, and AI try-on processing is not implemented. The admin panel supports catalog and inventory management, costume image galleries, user administration, inquiry status updates, and super-admin role permission assignments. No default administrator credentials are seeded; an operator must provision an administrator. Recent activity is recorded for admin actions; customer favorites, customer inquiry submission, and AI model functionality are not implemented; see [docs/customer-storefront.md](docs/customer-storefront.md), [docs/admin-panel.md](docs/admin-panel.md), [docs/image-storage.md](docs/image-storage.md), and [apps/mobile/README.md](apps/mobile/README.md). The backend exposes health, public catalog reads, registration/login/current-user APIs, refresh rotation/logout, password reset, admin management endpoints, image upload and gallery APIs, private customer photo APIs, and permission-filtered dashboard counts. Refresh tokens are stored hashed and rotated, and reuse revokes their full token family. Web refresh cookies use `HttpOnly`, `SameSite=Strict`, and CSRF double-submit validation; mobile clients receive refresh tokens in JSON for OS secure storage. The AI service exposes a health check. Cloud photo storage and AI model functionality remain out of scope.

The backend tests include an isolated PostgreSQL 16 instance that runs all Flyway migrations, validates the JPA schema, and checks persistence, authentication, token rotation/reuse, password reset, CSRF, permission boundaries, and image storage. Run them with `cd services/backend && mvn test`.
# designer-costume-app
