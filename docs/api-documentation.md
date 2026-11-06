# Backend REST API

## Architecture

The REST API uses the existing `com.designer.costume` Spring package:

- **Controller** maps HTTP routes, validates query/path parameters, and returns response DTOs.
- **Service** performs read-case decisions and is marked `@Transactional(readOnly = true)`.
- **Repository** executes Spring Data JPA queries against the existing Phase 2 schema.
- **Response DTOs** define stable API shapes and do not expose JPA entities.
- **Global exception advice** turns expected application/database errors into consistent HTTP responses.
- **Spring Security** validates bearer access tokens and applies database-backed permission checks.

Catalog read queries use JPQL constructor projections, so category and primary-image information is fetched without serializing lazy entity relationships. Admin write APIs use request/response DTOs. Migration V13 adds the minimal metadata table used by the administrative activity feed.

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

`GET /api/v1/admin/dashboard` requires an `ADMIN` or `SUPER_ADMIN` access token. The response contains active category and costume counts, available variants, active customers, inquiry totals, and active costume counts by category. Each field is populated only when the authenticated administrator has its corresponding view permission; fields without permission are `null`.

`GET /api/v1/admin/activity?limit=10` requires `USER_VIEW` and returns up to 50 of the latest successful administrative changes. Events record an action, resource type and ID, actor reference, and timestamp. Request bodies, passwords, and tokens are not recorded. Customer registration and read-only requests do not create activity events.

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

Registration, login, refresh, logout, current-user, and password-recovery endpoints are documented in [authentication-and-security.md](./authentication-and-security.md). The health endpoint and active catalog read endpoints are public. Administrative writes are under `/api/v1/admin` and require both an administrator role and the relevant permission.

### Admin catalog

| Method and path | Permission | Behavior |
| --- | --- | --- |
| `GET /api/v1/admin/categories?page=&size=&search=&active=` | `CATEGORY_VIEW` | Paginated categories; `active` may be true, false, or omitted |
| `GET /api/v1/admin/categories/{id}` | `CATEGORY_VIEW` | Category details, including inactive records |
| `POST /api/v1/admin/categories` | `CATEGORY_CREATE` | Create category; optional parent and HTTP(S) image URL |
| `PUT /api/v1/admin/categories/{id}` | `CATEGORY_UPDATE` | Replace category fields and active state |
| `DELETE /api/v1/admin/categories/{id}` | `CATEGORY_DELETE` | Soft-deactivate; rejected while active costumes use the category |
| `GET /api/v1/admin/costumes?page=&size=&search=&categoryId=&active=` | `COSTUME_VIEW` | Paginated costumes, including inactive records |
| `GET /api/v1/admin/costumes/{id}` | `COSTUME_VIEW` | Costume details and primary image URL |
| `POST /api/v1/admin/costumes` | `COSTUME_CREATE` | Create costume and optional primary image URL |
| `PUT /api/v1/admin/costumes/{id}` | `COSTUME_UPDATE` | Update costume fields and active state |
| `DELETE /api/v1/admin/costumes/{id}` | `COSTUME_DELETE` | Soft-deactivate |
| `GET /api/v1/admin/costumes/{costumeId}/images` | `COSTUME_VIEW` | List gallery metadata |
| `POST /api/v1/admin/costumes/{costumeId}/images` | `COSTUME_UPDATE` | Multipart image upload (`file`) |
| `PUT /api/v1/admin/costumes/{costumeId}/images/{imageId}/primary` | `COSTUME_UPDATE` | Set the primary image |
| `PUT /api/v1/admin/costumes/{costumeId}/images/order` | `COSTUME_UPDATE` | Reorder all gallery images |
| `DELETE /api/v1/admin/costumes/{costumeId}/images/{imageId}` | `COSTUME_UPDATE` | Delete gallery image |

Category names and costume SKUs must be unique. An active costume must reference an active category. Category parent cycles are rejected. Uploads accept JPEG, PNG, and WebP files up to 10 MiB. Uploaded image content is available at `GET /api/v1/costumes/{costumeId}/images/{imageId}/content` for active costumes.

### Customer photo library

The following endpoints require an authenticated `CUSTOMER` access token. Ownership is derived from the token; no user ID is accepted.

| Method and path | Behavior |
| --- | --- |
| `GET /api/v1/customer/photos` | List the signed-in customer’s photo metadata |
| `POST /api/v1/customer/photos` | Multipart upload with field `file` |
| `GET /api/v1/customer/photos/{photoId}/content` | Read private photo content (`no-store`) |
| `DELETE /api/v1/customer/photos/{photoId}` | Delete the customer’s photo and content |

Photo deletion hides the metadata immediately and removes the stored file after the database transaction commits. See [image storage and privacy](./image-storage.md) for local filesystem configuration and the Compose upload volume.

### Admin variant inventory

| Method and path | Permission | Behavior |
| --- | --- | --- |
| `GET /api/v1/admin/colors`, `GET /api/v1/admin/sizes` | `COSTUME_VIEW` | List all options, including inactive records |
| `POST /api/v1/admin/colors`, `POST /api/v1/admin/sizes` | `COSTUME_CREATE` | Create color or size |
| `PUT /api/v1/admin/colors/{id}`, `PUT /api/v1/admin/sizes/{id}` | `COSTUME_UPDATE` | Update attributes or active state |
| `GET /api/v1/admin/costumes/{costumeId}/variants` | `COSTUME_VIEW` | List all variants, including unavailable variants |
| `POST /api/v1/admin/costumes/{costumeId}/variants` | `COSTUME_CREATE` | Create variant with SKU, color, size, price, stock, and availability |
| `PUT /api/v1/admin/costumes/{costumeId}/variants/{variantId}` | `COSTUME_UPDATE` | Update variant inventory and availability |

Variant SKUs and color-size combinations are unique. Available variants require active color and size records. Public catalog variant reads continue to return only available variants.

### Admin users and inquiries

| Method and path | Permission | Behavior |
| --- | --- | --- |
| `GET /api/v1/admin/users?page=&size=&search=&active=` | `USER_VIEW` | Search and paginate users |
| `GET /api/v1/admin/users/{id}` | `USER_VIEW` | Safe user details; password hashes are never returned |
| `POST /api/v1/admin/users` | `USER_CREATE` | Create user; roles are limited to permissions the actor effectively holds |
| `PUT /api/v1/admin/users/{id}` | `USER_UPDATE` | Update profile, role assignments, and active state |
| `DELETE /api/v1/admin/users/{id}` | `USER_DELETE` | Deactivate account and revoke refresh sessions |
| `GET /api/v1/admin/inquiries?page=&size=&search=&status=` | `INQUIRY_VIEW` | Search and paginate inquiries |
| `GET /api/v1/admin/inquiries/{id}` | `INQUIRY_VIEW` | Inquiry details |
| `PATCH /api/v1/admin/inquiries/{id}/status` | `INQUIRY_UPDATE` | Change status to a defined `InquiryStatus` |

The last active super administrator cannot be deactivated or demoted. Updating a user to inactive revokes all refresh sessions; protected requests also reject tokens for inactive users.

### Admin roles and permissions

`GET /api/v1/admin/roles`, `POST /api/v1/admin/roles`, `PUT /api/v1/admin/roles/{id}`, and `GET /api/v1/admin/permissions` are restricted to the `SUPER_ADMIN` role. Permission definitions are read-only and migration-seeded. Removing a permission from a role revokes the corresponding snapshotted user grants; adding a role permission does not automatically add it to existing users. A newly assigned role receives a permission snapshot at assignment time.

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
| `413 Payload Too Large` | Uploaded image exceeds the configured request/file size limit |
| `404 Not Found` | Requested active category or costume does not exist |
| `401 Unauthorized` | Missing, invalid, expired, or revoked credentials on protected endpoints |
| `403 Forbidden` | Authenticated user lacks the required permission or CSRF validation fails |
| `409 Conflict` | Database integrity constraint conflict |
| `500 Internal Server Error` | Unexpected server failure; details are logged, not returned |

Validation errors return the first field/parameter validation message. Duplicate unique values return `409 Conflict`. Deactivation endpoints preserve records rather than hard-deleting them.

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
