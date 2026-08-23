# Local map, Razorpay, and SMS integrations

The admin portal includes an interactive OpenStreetMap station map, Razorpay Checkout for customer invoices, and MSG91 SMS delivery.

## Station map

No API key is required for local development.

1. Open **Stations → Stations** and add a station.
2. Enter valid latitude and longitude values.
3. Open **Stations → Map View**.
4. Select a marker to see the station status, address, and directions link.

Stations without coordinates are listed as hidden below the map. OpenStreetMap's public tile service is suitable for local testing; select a production tile provider before high-volume public deployment.

## Razorpay test payments

Use Razorpay **test mode** keys during local development. Never commit a Key Secret.

1. Sign in as an administrator.
2. Open **Payments → Payment Gateways**.
3. Select `RAZORPAY`, enable it, and enter the test **Key ID** and **Key Secret**.
4. Save the gateway settings.
5. Issue an invoice to a customer.
6. Sign in as that customer and open **Payments → Invoices**.
7. Select **Pay online**.

The payment service creates the Razorpay order. The frontend opens Razorpay Checkout, and the backend verifies the returned HMAC signature before marking the payment and invoice as paid. Refunds for successful Razorpay payments call Razorpay before changing the local payment status.

Official setup reference: <https://razorpay.com/docs/api/orders/>

## MSG91 SMS

For Indian SMS, create and approve the required sender/template in MSG91 and complete the applicable DLT registration. The template must expose a variable whose name matches `MSG91_MESSAGE_VARIABLE` (the default is `message`).

Set the credentials in the PowerShell window before starting the backend:

```powershell
$env:MSG91_AUTH_KEY = "your-msg91-auth-key"
$env:MSG91_TEMPLATE_ID = "your-msg91-flow-template-id"
$env:MSG91_MESSAGE_VARIABLE = "message"

powershell.exe -NoProfile -ExecutionPolicy Bypass -File ".\tools\start-all.ps1" -DatabasePassword "your-database-password"
```

Then open **Content → Notifications**:

1. Select `SMS`.
2. Enter the mobile number with country code. A 10-digit Indian number automatically receives country code `91`.
3. Enter the approved template's message-variable value.
4. Queue the notification and select **Send**.
5. The table shows `SENT` with the MSG91 request ID, or `FAILED` with a provider/configuration error. Failed messages can be retried.

Official API reference: <https://docs.msg91.com/sms/send-sms>

## Rebuild after pulling these changes

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File ".\tools\stop-all.ps1" -ForceAllJava

& "C:\software\maven\apache-maven-3.9.16\bin\mvn.cmd" "-Dmaven.repo.local=$PWD\.maven-repository" clean package -DskipTests

powershell.exe -NoProfile -ExecutionPolicy Bypass -File ".\tools\start-all.ps1" -DatabasePassword "your-database-password"

Set-Location .\frontend\admin-portal
npm.cmd install
npm.cmd run dev
```

Flyway applies the new payment database migration automatically when the payment service starts.
