# Customer storefront

Phase 6 adds a customer-facing Next.js storefront alongside the Phase 5 admin panel. Both experiences share the same root layout, Axios client, in-memory access-token handling, and authentication context.

## Routes

| Route | Access | Purpose |
| --- | --- | --- |
| `/` | Public | Boutique home, live category and costume highlights, try-on and contact explanations |
| `/categories` | Public | Paginated active category cards |
| `/costumes` | Public | Searchable, category-filtered costume catalog with debounced URL-backed filters and pagination |
| `/costumes/[id]` | Public | Costume details, image gallery, and available color/size variants |
| `/login` | Public | Shared customer/admin sign-in using the existing Phase 4 endpoint |
| `/register` | Public | Creates a CUSTOMER account through the existing registration endpoint |
| `/favorites` | CUSTOMER | Protected page explaining that saved favorites need missing backend APIs |
| `/try-on` | CUSTOMER | Local photo preparation and opt-in private photo-library management; no AI inference |
| `/contact` | Public | Configured shop contact details and a disabled inquiry form with a clear API limitation |
| `/admin/**` | ADMIN / SUPER_ADMIN | Existing Phase 5 workspace |

Storefront routes are grouped under `src/app/(store)/`; the route group does not appear in the URL. Admin routes remain under `(admin)` and auth routes share `(auth)`.

## Architecture

- `components/storefront/` contains the shop header/footer, category and costume cards, catalog grid, variant selector, listing, state views, and product detail experience.
- `components/try-on/` contains the local photo picker and preparation workspace.
- `services/api/categoryApi.js` and `costumeApi.js` are shared with the admin workspace.
- `services/api/authApi.js`, `apiClient.js`, and `context/AuthContext.js` remain the single auth implementation for both experiences.
- Public catalog pages fetch active records from the backend. UI cards do not invent product names, images, categories, or prices.

## API integrations

| Operation | Endpoint |
| --- | --- |
| Active categories | `GET /api/v1/categories?page=&size=&search=` |
| Active costumes | `GET /api/v1/costumes?page=&size=&search=&categoryId=` |
| Active costume detail | `GET /api/v1/costumes/{id}` |
| Available variants | `GET /api/v1/costumes/{costumeId}/variants` |
| Active costume gallery metadata and image content | `GET /api/v1/costumes/{costumeId}/images` |
| Private customer photo library | Authenticated `GET/POST/DELETE /api/v1/customer/photos` |
| Customer registration | `POST /api/v1/auth/register` |
| Login / session refresh / logout / current user | Existing Phase 4 `/api/v1/auth/*` endpoints |

The catalog GET endpoints are public and return only active categories/costumes and available variants. Backend sorting remains the existing name order. Catalog DTOs do not provide a currency, so displayed prices are numeric and explicitly labeled as currency-unspecified. Existing primary image URLs remain compatible; uploaded gallery images are served by the backend.

## Authentication

Registration calls the existing server endpoint, which always assigns the CUSTOMER role. Login accepts either customer or administrator credentials. Access tokens remain in memory; the refresh token remains an HttpOnly cookie. The existing shared Axios client adds the access token to requests, includes credentials, sends the CSRF double-submit header for refresh/logout, and shares concurrent refresh requests. `/favorites` and `/try-on` redirect unauthenticated visitors to `/login` and require the CUSTOMER role. Admin routes still enforce administrative role and permissions in the UI and backend.

The catalog is readable without a session; protected profile and admin APIs remain protected. Browser cookies require same-site hosting, exact configured backend CORS origin, and HTTPS with secure cookies in production.
Authenticated visitors are redirected away from login and registration pages to the storefront.

## Feature limitations and missing backend APIs

- **Favorites:** no favorite list/add/remove endpoints; no localStorage substitute is used.
- **Inquiries:** admin listing and status updates are available, but there is no customer inquiry-submission endpoint; contact form submission is disabled. Optional direct email/phone links work when configured.
- **Try-on:** photo upload to the private account library is opt-in. Uploaded photos are not sent to the AI service. AI inference and generated results do not exist.
- **Gallery:** admins manage costume images in the admin gallery; customers can browse active costume galleries.
- **Inventory and product actions:** no add-to-cart, checkout, stock quantity, or costume write APIs.
- **Price currency:** catalog API does not include currency metadata.

These are intentional limitations, not successful-but-empty operations.

## Contact environment values

Set optional public configuration in `apps/web/.env.local`:

```dotenv
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080/api/v1
NEXT_PUBLIC_SHOP_ADDRESS=
NEXT_PUBLIC_SHOP_PHONE=
NEXT_PUBLIC_SHOP_EMAIL=
```

Only public shop contact details belong in `NEXT_PUBLIC_*`; never add secrets.

## Run and verify

From `apps/web`:

```bash
npm run dev
npm run lint
npm run build
```

The storefront runs at `http://localhost:3000`. If a different port is used, add that exact origin to backend `CORS_ALLOWED_ORIGINS`. The repository does not currently configure a frontend unit-test runner; build and lint are the available frontend checks. Backend integration tests cover the public catalog API behavior and customer auth.
