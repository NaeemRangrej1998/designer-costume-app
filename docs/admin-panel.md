# Admin web panel

Phase 5 adds a permission-aware Next.js admin workspace. The frontend integrates only with endpoints that exist in the Spring Boot API. It does not create simulated records or claim a write operation succeeded when there is no corresponding backend API.

## Routes

| Route | Access | Behavior |
| --- | --- | --- |
| `/login` | Public | Authenticates with `POST /api/v1/auth/login`; rejects non-administrative accounts for the admin area |
| `/admin` | `ADMIN` or `SUPER_ADMIN` | Live, permission-filtered catalog/customer/inquiry counts and costume distribution |
| `/admin/categories` | Administrative role + `CATEGORY_VIEW` | Search and paginate active categories; displays parent and image links |
| `/admin/costumes` | Administrative role + `COSTUME_VIEW` | Search and paginate active costumes; optional category filter when `CATEGORY_VIEW` is granted |
| `/admin/costumes/[id]` | Administrative role + `COSTUME_VIEW` | Costume detail and available variants |
| `/admin/users` | Administrative role + `USER_VIEW` or `USER_CREATE` | Explains that the backend has no user directory endpoint; links to creation when permitted |
| `/admin/users/create` | Administrative role + `USER_CREATE` | Creates a user through the existing API; the backend remains authoritative for role assignment |

Other requested sections (colors, sizes, full variants, customers, roles, permissions, inquiries, and settings) are not presented as functional navigation items because the backend has no corresponding permitted APIs.

## Frontend structure

- `src/context/AuthContext.js` owns the in-memory access token, authenticated user, session restoration, and logout.
- `src/services/api/` contains the shared Axios client and endpoint-specific functions.
- `src/components/layout/` contains the responsive admin shell and permission-filtered navigation.
- `src/components/common/` contains the protected-route, table, pagination, loading, empty, and page-header components.
- Route folders under `src/app/` contain page-level data loading and forms.

## Authentication and authorization

Login calls the existing `POST /api/v1/auth/login` endpoint. The web client sends requests with credentials enabled. The backend sets the refresh token in an HttpOnly cookie; JavaScript never reads or stores that token. Access tokens stay in the shared JavaScript module's memory and are attached as bearer headers. A full reload restores the session by sending a CSRF-protected refresh request, then loading `GET /api/v1/auth/me`.

The readable `csrf_token` cookie is sent as `X-CSRF-TOKEN` for refresh and logout. Its path is `/` so the admin pages can read the double-submit value; the HttpOnly refresh cookie remains scoped to `/api/v1/auth`. A previously issued CSRF cookie on the narrower path is expired when a new session is written. Keep the web and API on the same host/site for this cookie-based setup. For local HTTP development, configure `AUTH_COOKIE_SECURE=false`; use HTTPS and `true` in production.

The Axios response interceptor shares one refresh request for simultaneous expired-access requests, retries each original request at most once, and clears in-memory authentication if refresh returns 401. The UI hides links and protects pages based on the user's `roles` and `permissions` from `/auth/me`; this is only a usability layer. Spring Security and service checks remain the actual authorization boundary.

## Verified backend API coverage

| Feature | Existing API integrated |
| --- | --- |
| Login / session restoration / current user / logout | `POST /api/v1/auth/login`, `POST /api/v1/auth/refresh`, `GET /api/v1/auth/me`, `POST /api/v1/auth/logout` |
| Category reads | `GET /api/v1/categories?page=&size=&search=`, `GET /api/v1/categories/{id}` |
| Costume reads | `GET /api/v1/costumes?page=&size=&search=&categoryId=`, `GET /api/v1/costumes/{id}` |
| Available costume variants | `GET /api/v1/costumes/{costumeId}/variants` |
| Administrative user create | `POST /api/v1/admin/users` |
| Dashboard summary | `GET /api/v1/admin/dashboard` |

List APIs return only active categories/costumes. Category and costume list pages therefore show the active status but cannot manage inactive records.

## Backend API gaps (buttons and mutations intentionally omitted)

- Inquiry management, inquiry status updates, and recent activity feed. The dashboard shows live inquiry totals only to users with `INQUIRY_VIEW`.
- Category create, update, activate/deactivate, and image mutation.
- Costume create, update, activate/deactivate, category-filter metadata for users who cannot read categories, and image upload.
- Color and size list/create/update/availability APIs.
- Variant list/filter/create/update/stock/availability APIs. The existing per-costume endpoint only returns available variants.
- Customer list and detail APIs.
- User list/detail/update/activate/deactivate APIs. Only create is present.
- Role list/detail/create/update and role-permission assignment APIs.
- Permission list/detail/group APIs.
- Inquiry list/detail/search/status-update APIs.
- Settings APIs.

Adding these features requires separately authorized backend endpoints, request/response DTOs, validation, and tests. The admin panel does not call invented routes.

## Dashboard summary API

`GET /api/v1/admin/dashboard` requires an `ADMIN` or `SUPER_ADMIN` access token. The response includes live counts from the database, and each metric is returned as `null` unless the current administrator has the corresponding view permission:

- `CATEGORY_VIEW`: active category count.
- `COSTUME_VIEW`: active costume and available-variant counts.
- `CATEGORY_VIEW` plus `COSTUME_VIEW`: active costume counts grouped by active category.
- `USER_VIEW`: active customer count.
- `INQUIRY_VIEW`: total and new inquiry counts.

Recent activity and inquiry listing/status management are not available because their APIs are not implemented. Empty counts reflect the database; no dashboard values are fabricated.

## Configuration and running

Set `NEXT_PUBLIC_API_BASE_URL` in `apps/web/.env.local`, for example:

```dotenv
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080/api/v1
```

Configure backend `CORS_ALLOWED_ORIGINS` to the exact web origin (for example `http://localhost:3000`) and set `AUTH_COOKIE_SECURE=false` only for local HTTP. The browser requires credentials-enabled CORS and the host/site-scoped cookie behavior described above.

Run the backend and database as documented in [development-setup.md](./development-setup.md), then start the web app:

```bash
cd apps/web
npm install
npm run dev
```

Open `http://localhost:3000/login`. The backend must already contain an administrative account with appropriate permissions; there is no public admin-bootstrap endpoint.

There is no default admin email or password in the repository. Use credentials provisioned by the system operator. If no admin exists, the controlled bootstrap must be performed by an authorized backend/database operator; the login screen and public registration endpoint do not grant administrative roles.

## Testing

The web package does not currently have a configured component/unit-test runner. Phase 5 adds no test dependency. Run the existing ESLint/build checks with `npm run lint` and `npm run build` in `apps/web`. Backend cookie/session behavior is covered by the existing isolated PostgreSQL integration test suite.
