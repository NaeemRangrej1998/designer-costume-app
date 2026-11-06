# Image storage and customer photo privacy

The backend stores uploaded image bytes on its local filesystem. PostgreSQL stores image metadata and generated storage keys; uploaded filenames are never used as storage paths. Supported formats are JPEG, PNG, and WebP, with a default 10 MiB per-file limit. Format is determined from the file signature, not the filename or client-supplied MIME type. JPEG and PNG dimensions are recorded when readable; WebP dimensions may be absent.

## Costume galleries

Administrators with costume view/update permissions can upload, list, reorder, set the primary image, and delete costume images using `/api/v1/admin/costumes/{costumeId}/images`. Public catalog clients can list and read images only for active costumes. Legacy externally hosted image URLs remain compatible and are not moved automatically.

## Customer photos

Customers can upload, list, read, and delete their own photos under `/api/v1/customer/photos`. The API derives ownership from the authenticated user; it never accepts a user ID in the request. Photo content is served through authenticated endpoints with `Cache-Control: no-store`; metadata does not expose a filesystem path or storage key. Deletion hides the metadata immediately and removes the file after the database transaction commits. Try-on uploads are not sent to the AI service.

Web and mobile clients only save a selected local photo to the account library after an explicit user action. The AI model/inference integration remains out of scope.

## Local and Docker configuration

For a host-run backend, configure `STORAGE_ROOT` (default `./storage`, relative to the backend working directory) and `MAX_FILE_SIZE`. Keep the directory on a persistent disk and include it in backups as appropriate. The directory is ignored by Git.

The Compose backend mounts the named `uploads_data` volume at `/app/storage`. Compose starts the API with the database after PostgreSQL reports healthy. Set a non-empty `DB_PASSWORD` and a production-grade `JWT_SECRET` in the root environment before running Compose; do not commit `.env` files.

```bash
docker compose -f infrastructure/docker-compose.yml up -d --build
```

Uploaded user photos are sensitive personal data. Restrict access to the host/volume, use HTTPS outside local development, define a retention policy, and protect backups accordingly.
