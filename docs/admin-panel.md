# Admin web panel

Phase 5 provides a permission-aware Next.js admin workspace backed by permission-checked Spring Boot APIs. The frontend does not create simulated records or claim a write operation succeeded when an API fails.

## Routes

| Route | Access | Behavior |
| --- | --- | --- |
| `/login` | Public | Authenticates with `POST /api/v1/auth/login`; rejects non-administrative accounts for the admin area |
| `/admin` | `ADMIN` or `SUPER_ADMIN` | Live, permission-filtered counts, costume distribution, and recent activity for users with `USER_VIEW` |
| `/admin/categories` | `CATEGORY_VIEW` | Search, filter by active state, edit, create, and deactivate categories |
| `/admin/costumes` | `COSTUME_VIEW` | Search, filter by category/status, edit, create, and deactivate costumes |
| `/admin/costumes/[id]` | `COSTUME_VIEW` | Costume details and all variants; create/update stock, price, image URL, and availability |
| `/admin/catalog-options` | `COSTUME_VIEW` | Create/update colors and sizes; activate/deactivate them |
| `/admin/users` | `USER_VIEW` or `USER_CREATE` | Search/paginate users, update users and roles, deactivate users, and create users as allowed |
| `/admin/inquiries` | `INQUIRY_VIEW` | Search/paginate inquiries and update status with `INQUIRY_UPDATE` |
| `/admin/roles` | `SUPER_ADMIN` role | Create roles and assign/remove the predefined permission set |
| `/admin/users/create` | `USER_CREATE` | Creates a user; server validates role assignments against the administrator's effective permissions |

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

## Backend API coverage

| Feature | Existing API integrated |
| --- | --- |
| Login / session restoration / current user / logout | `POST /api/v1/auth/login`, `POST /api/v1/auth/refresh`, `GET /api/v1/auth/me`, `POST /api/v1/auth/logout` |
| Public catalog reads | `GET /api/v1/categories`, `GET /api/v1/costumes`, and detail/available-variant GET routes |
| Category administration | `GET/POST /api/v1/admin/categories`, `GET/PUT/DELETE /api/v1/admin/categories/{id}` |
| Costume administration | `GET/POST /api/v1/admin/costumes`, `GET/PUT/DELETE /api/v1/admin/costumes/{id}` |
| Colors / sizes | `GET/POST /api/v1/admin/colors`, `PUT /api/v1/admin/colors/{id}`; corresponding `/sizes` routes |
| Inventory | `GET/POST /api/v1/admin/costumes/{costumeId}/variants`, `PUT /api/v1/admin/costumes/{costumeId}/variants/{variantId}` |
| Users | `GET/POST /api/v1/admin/users`, `GET/PUT/DELETE /api/v1/admin/users/{id}` |
| Inquiries | `GET /api/v1/admin/inquiries`, `GET /api/v1/admin/inquiries/{id}`, `PATCH /api/v1/admin/inquiries/{id}/status` |
| Roles / permission catalog | `GET/POST /api/v1/admin/roles`, `PUT /api/v1/admin/roles/{id}`, `GET /api/v1/admin/permissions` (SUPER_ADMIN only) |
| Dashboard summary and activity | `GET /api/v1/admin/dashboard`, `GET /api/v1/admin/activity?limit=10` |

Administrative list APIs can filter active and inactive records. `DELETE` on a category, costume, or user is a soft deactivation; records are retained. Color and size availability is managed by updating their `active` flag. Category/costume images use URLs; binary file upload/storage is not configured.

## Remaining limitations

- The permission catalog is read-only and seeded by database migrations; arbitrary permission definitions cannot be created through the UI.
- Role and permission management is restricted to `SUPER_ADMIN`. Existing user grants are snapshots: removing a role permission revokes it from assigned users; adding a permission does not silently grant it to existing users. Newly added permissions are snapshotted only when a role is newly assigned.
- Admins can update inquiry status, but customer inquiry submission remains unavailable in the storefront.
- The activity feed records successful administrative create/update/deactivate actions and inquiry status changes. It stores action/resource identifiers and the actor reference, not request bodies, passwords, or tokens.
- No configurable application-settings API is defined.
- Catalog image management accepts HTTP(S) URLs only; there is no binary upload or cloud image storage.
- The web UI does not configure currency; prices are shown as currency-unspecified values.

## Dashboard summary API

`GET /api/v1/admin/dashboard` requires an `ADMIN` or `SUPER_ADMIN` access token. The response includes live counts from the database, and each metric is returned as `null` unless the current administrator has the corresponding view permission:

- `CATEGORY_VIEW`: active category count.
- `COSTUME_VIEW`: active costume and available-variant counts.
- `CATEGORY_VIEW` plus `COSTUME_VIEW`: active costume counts grouped by active category.
- `USER_VIEW`: active customer count.
- `INQUIRY_VIEW`: total and new inquiry counts.

`GET /api/v1/admin/activity?limit=10` returns the latest successful administrative changes and requires `USER_VIEW`. The limit must be between 1 and 50. Empty counts and activity reflect the database; no values are fabricated.

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

The web package does not currently have a configured component/unit-test runner. Run the existing ESLint/build checks with `npm run lint` and `npm run build` in `apps/web`. The isolated PostgreSQL integration suite covers admin catalog writes and role-permission snapshot revocation.
