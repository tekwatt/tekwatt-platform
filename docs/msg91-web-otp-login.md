# MSG91 OTP sign-in for the admin web app

The web sign-in page keeps email/password and adds **Sign in with email OTP**. This path is for existing TekWatt accounts only. The MSG91 widget sends and checks the code, then TekWatt's auth service verifies the widget access token with MSG91 before issuing a TekWatt session. An OTP result from the browser alone is never accepted.

An administrator can enter the values in **Settings → General Settings → Login OTP provider**. The form stores the widget token and server Authkey encrypted in the auth database and never displays saved secrets again. To enable secure storage, the auth service must already have a stable `SMTP_CREDENTIALS_ENCRYPTION_KEY` (the same 32-byte Base64 key used for SMTP credentials). Do not rotate this key without migrating encrypted settings.

Alternatively, configure the auth service with three environment variables, preferably Azure Container Apps secrets for the token and Authkey:

| Variable | Source |
| --- | --- |
| `MSG91_OTP_WIDGET_ID` | MSG91 OTP Widget → Client Side Integration → `widgetId` |
| `MSG91_OTP_WIDGET_TOKEN` | MSG91 OTP Widget → Client Side Integration → `tokenAuth` (restricted browser widget token) |
| `MSG91_OTP_SERVER_AUTH_KEY` | MSG91 OTP Widget → Server Side Integration → account Authkey; **server only** |

Do not use `MSG91_AUTH_KEY` from the notification service as a browser value, and do not put `MSG91_OTP_SERVER_AUTH_KEY` in the frontend, source control, or a chat message. The widget must allow email verification. Disable MSG91 demo credentials for production and restrict the widget to the TekWatt website origin if that setting is available.

Deploy the auth service, API gateway, and admin portal together. The auth service applies database migrations `V5__used_otp_tokens.sql`, `V6__msg91_otp_settings.sql`, and `V7__verified_user_phone.sql`. Test with an existing account email, then confirm a wrong email, an invalid/expired token, and a repeated token cannot sign in. An unconfigured provider returns a service-unavailable response and cannot issue a session.

Phone OTP requires a separate verified binding. Sign in with the existing account's email and password, open **Account Settings → Phone OTP sign-in**, enter the number with country code, and complete MSG91 verification. Only then can **Sign in with phone OTP** issue a session for that account. A number typed into an administrator or customer profile is not a login credential. Each number can be linked to only one account, and changing it requires a new OTP while signed in. The phone verification response must identify the exact number; the server rejects ambiguous or mismatched results.

Before using phone OTP in production, verify the new database migration succeeds on the production database, the configured MSG91 widget has mobile verification enabled, and the signed-in account has the intended server-side role. Do not treat a frontend “Admin” label as proof that the auth-service token has the ADMIN role.

## Administrator account activation

Creating a login through the public registration endpoint initially gives it the safe `DRIVER` role. Creating an administrator directory profile alone does not grant server-side administrator access. The **Admins → Administrators** page now activates server access after it creates an active `ADMIN` profile linked to that login. For an existing profile with an `authUserId`, use **Activate access** in the same table. This action is allowed only for an authenticated administrator (or an explicitly configured bootstrap administrator) and only when the directory has an active matching `ADMIN` record for the same user ID and email. Activation revokes that user's existing sessions; they must sign in again.

For a legacy bootstrap administrator whose auth-service role is still `DRIVER`, set `SMTP_ADMIN_EMAILS` on the auth service to that account's exact email and ensure it has an active administrator directory record for the workspace. This bootstrap setting does not itself convert the role; an authorized administrator must use **Activate access**. Remove a temporary bootstrap entry after the role has been activated and sign-in verified.

Do not link a phone number merely because it is present in an administrator or customer directory profile. The account owner must first sign in and complete **Account Settings → Phone OTP sign-in**, which proves control of the number before it can be used for login.
