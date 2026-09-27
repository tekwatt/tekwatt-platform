# TekWatt Admin Portal

React and TypeScript operations portal for the TekWatt EV charging platform.

## Run locally

```powershell
npm install
npm run dev
```

Open `http://localhost:3000`. The portal starts with demo data. Copy `.env.example`
to `.env` and set `VITE_USE_DEMO_DATA=false` when the backend endpoints are ready.

Set `VITE_IDLE_TIMEOUT_MINUTES` to control automatic logout after inactivity. It defaults to 15 minutes and displays a warning during the final minute.

Set `VITE_API_TIMEOUT_SECONDS` to control how long the portal waits for a backend service before displaying a friendly timeout message. The default is 12 seconds.

Demo login: `admin@tekwatt.in` / `admin123`

## Google Maps

In **Settings → General Settings → Add provider**, choose **Google Maps** and enter a Google Maps JavaScript browser key. The selection is saved for the current workspace; **Stations → Map View** then uses Google Maps instead of OpenStreetMap. Enable billing and the **Maps JavaScript API** in Google Cloud, and restrict this browser key to the portal's website origin (including the Azure-hosted web address). The key is public in browser requests, so do not use a server-only secret key. If the key changes while the portal is open, reload the page.

The native Android app uses a separate Android-restricted key, configured at build time; see `frontend/mobile-app/README.md`. Address lookup in the station form still uses the existing OpenStreetMap search service.
