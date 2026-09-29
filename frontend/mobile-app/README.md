# TekWatt Nexus Android app

Single Expo/React Native app for TekWatt drivers and linked CPO operators. The server identifies the account after sign-in and opens the matching experience.

## Included flows

- Secure login and workspace-controlled registration (disabled until a default workspace is configured)
- Dashboard with wallet, energy, active sessions, and available stations
- Station search, connector selection, and OCPP remote-start with charger-confirmed session tracking
- Active-session monitoring and OCPP remote-stop; final meter values come from the charger
- Wallet and payment history
- Wallet ledger and balance activity
- Reservations with customer cancellation
- Invoices, payment status, and in-app Razorpay checkout in an Android development/production build
- Assigned RFID charging cards
- Support ticket creation, conversation and lifecycle status
- Editable customer profile, with sign-in email locked pending an identity-change flow
- Signed-in device review and remote session revocation
- Automatic access-token renewal
- Google station map and directions using saved latitude and longitude
- Live station and charging refresh while screens are open
- In-app invoice charging details and a shareable text summary (not a PDF tax invoice)
- Public privacy and account-deletion pages linked from the app when `EXPO_PUBLIC_CUSTOMER_PORTAL_URL` is set; deletion requests go to support@tekwatt.in
- CPO overview for partner-linked accounts: assigned chargers, faults, live/recent sessions and completed energy. Partner linking must include `authUserId`; matching an email alone does not grant operator access.

## API connection

The Android emulator uses `http://10.0.2.2:8080` by default. For a physical phone, create `.env` and use the Windows computer's LAN address:

```env
EXPO_PUBLIC_API_BASE_URL=http://192.168.1.25:8080
EXPO_PUBLIC_DEFAULT_TENANT_ID=optional-workspace-uuid
```

The phone and Windows computer must be on the same network. Windows Firewall must allow inbound access to the API Gateway and Expo ports.

## Run on Android

```powershell
Set-Location C:\Users\magpier\tekwatt\frontend\mobile-app
& "C:\Program Files\nodejs\npm.cmd" install
& "C:\Program Files\nodejs\npm.cmd" run android
```

Install Android Studio and create an emulator first, or connect an Android phone with USB debugging enabled.

## Verify

```powershell
& "C:\Program Files\nodejs\npm.cmd" run typecheck
& "C:\Program Files\nodejs\npm.cmd" run doctor
& "C:\Program Files\nodejs\npm.cmd" run export:android
```

## Build an installable Android APK

```powershell
Set-Location C:\Users\magpier\tekwatt\frontend\mobile-app
& "C:\Program Files\nodejs\npx.cmd" eas login
& "C:\Program Files\nodejs\npx.cmd" eas build --platform android --profile preview
```

The preview profile produces an APK for direct installation. The production profile produces an Android App Bundle for Google Play.

## Google Maps and in-app payment

For an installable Android build, set `GOOGLE_MAPS_ANDROID_API_KEY` in the build environment. Enable the **Maps SDK for Android** in Google Cloud, enable billing, and restrict the key to the Android package `in.tekwatt.nexus` and the signing certificate SHA-1. Expo Go can display Google Maps on Android without your own Android key, but an APK needs this configuration. Stations without valid latitude and longitude remain in the list but cannot be mapped.

In-app Razorpay payment uses the existing TekWatt payment service to create an order and verify its signature. Configure and enable Razorpay under Payment Gateways first. Install a native TekWatt development or preview APK for checkout; Expo Go cannot run the Razorpay native module. A customer can pay an issued invoice from **Invoices** or **Payment inbox**. The app checks for an already successful payment before opening checkout to avoid charging the invoice again.

The optional `EXPO_PUBLIC_CUSTOMER_PORTAL_URL` enables a browser payment link and QR in Payment inbox. It is a fallback; native checkout does not require it. Do not put Razorpay secrets in the mobile `.env` file.

## Release blockers

- Public signup must use an explicitly approved workspace UUID and a server-side enrollment flow. Do not publish a client-only workspace ID as the sole authorization control. Without the UUID, the Create account tab remains hidden.
- The current backend API routes do not consistently enforce token and tenant ownership. The mobile app filters driver data locally, but that is not a server-side security boundary. Add authorization to driver-visible data and OCPP command routes before a public release.
- The CPO overview API validates the active login and filters by the partner's charger ownership. Other existing APIs are still directly reachable through the gateway; harden those routes before publishing to customers or CPOs. CPO station setup, tariffs, remote control and settlements remain in the web portal.
- The public privacy and account-deletion page source is in `frontend/admin-portal/public/`. It must be reviewed and deployed to the configured portal URL before a store release. The deletion flow is a support request, not automatic deletion; the support team needs a verified fulfillment procedure.
- A final, downloadable tax invoice PDF and a full notification inbox are not available in this build. Invoice details and a text summary are available in-app.
- Build and test a signed Android package on a real device, including Google Maps key restrictions and Razorpay checkout, before submitting to Google Play.
