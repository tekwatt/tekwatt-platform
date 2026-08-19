# TekWatt Nexus Android app

Customer-facing Expo/React Native app for the TekWatt EV Charging Platform.

## Included flows

- Secure login and optional workspace-controlled registration
- Dashboard with wallet, energy, active sessions, and available stations
- Station search, connector selection, and charging-session start
- Active-session monitoring and stop flow
- Wallet and payment history
- Support ticket creation and lifecycle status
- Customer profile, workspace, gateway details, and logout

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
& "C:\Program Files\nodejs\npx.cmd" expo-doctor
```
