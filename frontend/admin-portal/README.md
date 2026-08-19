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

Set `VITE_API_TIMEOUT_SECONDS` to control how long the portal waits for a backend service before displaying a friendly timeout message. The default is 30 seconds.

Demo login: `admin@tekwatt.in` / `admin123`
