# Designer Costume Studio Mobile App

React Native and Expo customer app for Android and iOS. It uses the shared Spring Boot API; it does not create another backend or database.

## Install and run

Requirements: Node.js/npm and the Expo Go app or an Android/iOS development build.

```bash
cd apps/mobile
npm install
cp .env.example .env
npm start
```

Press `a` to open an Android emulator or `i` for the iOS simulator. A native development build is required to exercise SecureStore and photo-library permissions fully.

## Backend URL

Set `EXPO_PUBLIC_API_BASE_URL` in the local `.env` file:

- Android emulator: `http://10.0.2.2:8080/api/v1`
- iOS simulator: commonly `http://localhost:8080/api/v1`
- Physical phone: `http://<computer-LAN-IP>:8080/api/v1`; the phone and computer must be able to reach each other on the network.

Use the actual reachable host for your device. `localhost` on a physical phone refers to the phone, not the development computer. Configure optional public boutique details with `EXPO_PUBLIC_SHOP_ADDRESS`, `EXPO_PUBLIC_SHOP_PHONE`, and `EXPO_PUBLIC_SHOP_EMAIL`. Do not put secrets in `EXPO_PUBLIC_*` variables.

The backend must be running and reachable. Local HTTP may require cleartext development networking configuration on Android/device builds; use HTTPS for deployed services.

## Authentication and secure storage

The app sends `X-Client-Platform: mobile` to the existing auth endpoints. Access tokens are held in memory. Refresh tokens are stored with Expo SecureStore, sent in the body to the existing refresh/logout APIs, and rotated securely after refresh. A shared Axios refresh promise avoids concurrent refresh rotations. Tokens are never written to AsyncStorage or logged. On invalid refresh, local credentials are cleared; on network errors the stored refresh token is retained for retry.

Registration creates a `CUSTOMER` account. Admin accounts are not accepted as mobile customer sessions. Login, registration, forgot-password, reset-password, current-user, refresh, and logout use existing backend endpoints.

## Screens

- Bottom tabs: Home, Categories, Costumes, Favorites, and Profile.
- Stack screens: Costume Details, Try-On Preparation, Contact, Login, Register, and Forgot Password.
- Catalog pages call the public categories, costumes, costume-detail, and available-variant APIs.
- Costume details display the public costume image gallery.
- Try-on can upload a selected JPEG/PNG/WebP photo to the signed-in customer’s private backend photo library. Saved photos can be selected or deleted; no photo is sent to the AI service.

## Limitations

- No backend favorites endpoints exist, so favorites are clearly unavailable and are not saved locally.
- No customer inquiry submission API exists; the contact form is informational and does not send.
- Try-on is preparation only. The backend stores opt-in customer photos, but AI inference and generated results are not available.
- Catalog APIs return only available variants; unavailable combinations are not present in the response to display.
- The backend does not provide currency metadata.
- `apps/mobile` currently has no dedicated unit-test or lint runner. `npx expo export --platform android` validates the Metro Android bundle; it is not a substitute for testing on physical devices.
