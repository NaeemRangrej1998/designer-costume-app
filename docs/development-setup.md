# Development Setup

## Required tools

- Node.js 20.9+ and npm
- Java 21 JDK and Maven
- Python 3.11+ and pip
- Docker Engine and Docker Compose v2

## Configure local variables

At the project root, copy `.env.example` to `.env` and set a non-production local `DB_PASSWORD`. Compose reads this file automatically. Spring Boot does not load dotenv files itself, so export the database values into the shell running Maven. The web and AI service each include their own `.env.example`.

## Start each project

Start the database from the root:

```bash
docker compose -f infrastructure/docker-compose.yml up -d db
```

Run the backend from `services/backend`:

```bash
set -a
source ../../.env
set +a
mvn spring-boot:run
```

The application applies Flyway migrations on startup and validates entity mappings against the migrated schema. To enable development SQL statement logging:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Run backend tests from `services/backend`:

```bash
mvn test
```

Database integration tests use a fresh, isolated embedded PostgreSQL instance; they do not touch the local Docker database.

Run the web client from `apps/web`:

```bash
npm install
npm run dev
```

Run the mobile client from `apps/mobile`:

```bash
npm install
npm start
```

Run the AI service from `services/ai`:

```bash
python3 -m venv .venv
source .venv/bin/activate
python -m pip install -r requirements.txt
uvicorn app.main:app --reload --host 0.0.0.0 --port "${AI_SERVICE_PORT:-8001}"
```

Detailed service commands, environment variables, and URLs are listed in the root [README](../README.md).

## Health URLs

- Web: `http://localhost:3000`
- Backend API: `http://localhost:8080/api/v1/health`
- Spring Actuator: `http://localhost:8080/actuator/health`
- AI service: `http://localhost:8001/health`
- PostgreSQL: `localhost:5432`
