# Password-reset email setup

The web and mobile sign-in screens provide **Forgot password**. TekWatt emails an eight-digit, single-use code to the account email address. Codes expire after ten minutes, allow at most five attempts, and can be requested again after 60 seconds. A successful reset revokes refresh sessions; already-issued access tokens expire on their normal short lifetime.

## One-time server setup

The auth service needs `SMTP_CREDENTIALS_ENCRYPTION_KEY`: a stable Base64-encoded random 32-byte key. Store it as an Azure Container Apps secret and reference it as an environment variable on `auth-service`. Back up the key securely; losing or changing it makes the saved SMTP password unreadable. Do not put it in Git or enter it in the web form.

Users whose authentication role is `ADMIN` can manage SMTP. For a legacy administrator whose authentication role is `DRIVER`, set `SMTP_ADMIN_EMAILS` to the approved administrator email address(es), and set `ADMIN_SERVICE_URL` to the internal admin service. The service also verifies that such an account is an active administrator in the selected workspace. Do not add ordinary customer addresses to the allowlist.

## Information to get from the email provider

- SMTP server hostname, such as `smtp.example.com`
- Port and encryption: STARTTLS (often 587) or SSL/TLS (often 465)
- SMTP username and its password, app-specific password, or SMTP API key
- Verified sender email address; optional reply-to email address
- Confirmation that SMTP authentication and outbound mail are enabled for the account, and that the sender address is permitted

In **Settings → General Settings → Email delivery & password reset**, enter these values and select **Save email settings**. The password is encrypted in the auth database and is never returned to the browser. Then select **Send test email**; it goes only to the signed-in administrator's email. Confirm its arrival before asking a customer to reset a password.

The auth service can still use existing `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`, `SMTP_PASSWORD`, and `SMTP_FROM` environment variables when no database configuration is saved. A saved General Settings configuration takes precedence. No SMTP details are required in the mobile or web app build.

Deploy the auth-service and admin-portal changes together. The auth service applies database migrations V3 and V4 at startup. Do not tell users password reset is active until the test email and a real reset have succeeded in the target environment.
