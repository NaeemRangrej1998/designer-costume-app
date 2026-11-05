# Backend REST API

## Architecture

The REST API uses the existing `com.designer.costume` Spring package:

- **Controller** maps HTTP routes, validates query/path parameters, and returns response DTOs.
- **Service** performs read-case decisions and is marked `@Transactional(readOnly = true)`.
- **Repository** executes Spring Data JPA queries against the existing Phase 2 schema.
- **Response DTOs** define stable API shapes and do not expose JPA entities.
- **Global exception advice** turns expected application/database errors into consistent HTTP responses.
- **Spring Security** validates bearer access tokens and applies database-backed permission checks.

Catalog list and detail queries use JPQL constructor projections, so category and primary-image information is fetched in the same query without serializing lazy entity relationships. Available variants are also projected directly to a DTO. The Phase 2 schema and migrations are unchanged.

## Common response envelope

Every successful endpoint returns:

```json
{
  "success": true,
  "message": "Backend is running",
  "data": {
    "status": "UP"
  },
  "timestamp": "2026-11-05T10:00:00Z"
}
```

Error responses have the same envelope and use a null `data` value:

```json
{
  "success": false,
  "message": "Category not found: 00000000-0000-0000-0000-000000000000",
  "data": null,
  "timestamp": "2026-11-05T10:00:00Z"
}
```

Timestamps are UTC instants. Unexpected failures are logged on the server, while the response contains only a generic message, not SQL, credentials, or stack traces.

## Endpoints

### Health

`GET /api/v1/health`

Returns `200 OK` with `data.status = "UP"`.

### Admin dashboard

`GET /api/v1/admin/dashboard` requires an `ADMIN` or `SUPER_ADMIN` access token. The response contains active category and costume counts, available variants, active customers, inquiry totals, and active costume counts by category. Each field is populated only when the authenticated administrator has its corresponding view permission; fields without permission are `null`. Recent activity and inquiry management are not included.

Example:

```json
{
  "success": true,
  "message": "Dashboard summary fetched successfully",
  "data": {
    "activeCategories": 5,
    "activeCostumes": 10,
    "activeCustomers": 1,
    "availableVariants": 80,
    "totalInquiries": 0,
    "newInquiries": 0,
    "costumeDistribution": [
      { "categoryName": "Bridal", "costumeCount": 2 }
    ]
  },
  "timestamp": "2026-11-05T10:00:00Z"
}
```

### Authentication

Registration, login, refresh, logout, current-user, and password-recovery endpoints are documented in [authentication-and-security.md](./authentication-and-security.md). The health endpoint and active catalog read endpoints are public. Category and costume reads expose only active catalog data; administrative catalog writes are not part of the current API.

### Categories

`GET /api/v1/categories` (public)

Returns active categories, alphabetically by name. Optional query parameters:

| Parameter | Default | Limits / meaning |
| --- | --- | --- |
| `page` | `0` | Zero-based; must be nonnegative |
| `size` | `10` | Must be between 1 and 100 |
| `search` | omitted | Case-insensitive name substring; maximum 100 characters |

Example:

```text
GET /api/v1/categories?page=0&size=10&search=wedding
```

`GET /api/v1/categories/{id}` (public) returns one active category. Parent details are represented by `parentCategoryId` and `parentCategoryName`; children are not recursively serialized.

Example category `data`:

```json
{
  "id": "00000000-0000-0000-0000-000000000001",
  "name": "Bridal Lehenga",
  "description": null,
  "imageUrl": null,
  "parentCategoryId": "00000000-0000-0000-0000-000000000002",
  "parentCategoryName": "Wedding Wear"
}
```

### Costumes

`GET /api/v1/costumes` (public)

Returns active costumes, alphabetically by name. Optional query parameters:

| Parameter | Default | Limits / meaning |
| --- | --- | --- |
| `page` | `0` | Zero-based; must be nonnegative |
| `size` | `10` | Must be between 1 and 100 |
| `search` | omitted | Case-insensitive name substring; maximum 100 characters |
| `categoryId` | omitted | Category UUID |

Example:

```text
GET /api/v1/costumes?page=0&size=10&search=bridal&categoryId=00000000-0000-0000-0000-000000000002
```

`GET /api/v1/costumes/{id}` (public) returns one active costume. Both costume responses include basic costume fields, `categoryId`, `categoryName`, and nullable `primaryImageUrl`.

### Costume variants

`GET /api/v1/costumes/{costumeId}/variants` (public)

Returns variants where the variant, color, and size are active/available. Each item includes its color name and hex code, size name and display order, price, and availability. An inactive or missing costume returns `404 Not Found`.

Example item:

```json
{
  "id": "00000000-0000-0000-0000-000000000010",
  "costumeId": "00000000-0000-0000-0000-000000000011",
  "colorName": "Ruby Red",
  "colorHexCode": "#AA1122",
  "sizeName": "M",
  "sizeDisplayOrder": 2,
  "price": 975.00,
  "available": true
}
```

## Pagination

List endpoint `data` uses a consistent shape:

```json
{
  "content": [],
  "page": 0,
  "size": 10,
  "totalElements": 0,
  "totalPages": 0,
  "last": true
}
```

Page size is capped at 100. Invalid page/size/search parameters return `400 Bad Request`.

## Error status codes

| HTTP status | Meaning |
| --- | --- |
| `400 Bad Request` | Invalid pagination/filter parameter, invalid path/query type, or malformed request body |
| `404 Not Found` | Requested active category or costume does not exist |
| `401 Unauthorized` | Missing, invalid, expired, or revoked credentials on protected endpoints |
| `403 Forbidden` | Authenticated user lacks the required permission or CSRF validation fails |
| `409 Conflict` | Database integrity constraint conflict |
| `500 Internal Server Error` | Unexpected server failure; details are logged, not returned |

Validation errors return the first field/parameter validation message. No write APIs or request bodies are introduced in this phase.

## API documentation UI

Springdoc generates OpenAPI at `GET /v3/api-docs`. Swagger UI is available at:

```text
http://localhost:8080/swagger-ui/index.html
```

## Run and verify

From `services/backend`, export the environment values in the root `.env` file and start the API:

```bash
set -a
source ../../.env
set +a
mvn spring-boot:run
```

The database must be running and match the Phase 2 configuration. Flyway applies the existing migrations and Hibernate validates the schema. To run tests:

```bash
mvn test
```

Tests use isolated PostgreSQL 16 for persistence and API integration checks; they do not depend on production data.
