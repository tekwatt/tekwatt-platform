# Payment SMS, mobile inbox and QR

New completed charging sessions create an issued invoice. The customer web portal shows the invoice request and a locally generated QR code. The Android app has an Inbox tab listing the signed-in customer's issued/paid invoices, refreshing every 15 seconds while open. Its payment button opens the web checkout in the phone browser; a separate browser sign-in may be needed.

The QR encodes only the portal URL plus invoice/workspace IDs. It does not contain credentials, customer contact data, or authorization tokens. It is **not** a UPI QR and scanning does not collect money. No third-party QR-image service receives the link.

## Configuration

Charging-session service:

```text
NOTIFICATION_SERVICE_URL=http://notification-service
CUSTOMER_PORTAL_URL=https://YOUR-CUSTOMER-PORTAL
PAYMENT_SMS_ENABLED=true
```

SMS is disabled by default. Set the HTTPS URL to the real deployed customer portal, with no query string, fragment or credentials. Enable only after verifying customer phone numbers and the approved message template. Customers with no phone remain pending for operations to correct; their invoice and app inbox still work.

Notification service uses the existing MSG91 integration:

```text
MSG91_AUTH_KEY=<secret>
MSG91_TEMPLATE_ID=<approved-template>
MSG91_MESSAGE_VARIABLE=message
```

The configured template must support the actual payment message and link. Verify applicable provider/template requirements before enabling delivery. Keep credentials in Azure secrets, never Git.

Long messages/URLs may use multiple billable SMS segments. No SMS credits are included in the application.

Mobile build:

```text
EXPO_PUBLIC_CUSTOMER_PORTAL_URL=https://YOUR-CUSTOMER-PORTAL
```

Rebuild/restart Expo after changing this value. Missing/invalid configuration displays an explanation instead of a broken payment button. The web QR uses the current portal origin.

## Delivery and retries

SMS includes invoice number, amount/currency, tax, energy, due date and an invoice-page link. A deterministic invoice-based key reuses the notification on billing retries. A delivery lock prevents concurrent send calls from sending the same accepted notification twice. Failed sends retry up to the configured attempt limit (three); exhausted failures remain visible for operations, not silently marked delivered. A provider acceptance status is not proof the handset received the SMS. Network/provider ambiguity can still result in a duplicate SMS; exactly-once external delivery is not guaranteed.

This is SMS plus an in-app inbox, **not background push notifications**. The inbox is derived from issued invoices, so an SMS failure cannot hide the invoice. Paid invoices stop showing QR/payment actions.

## Before production

- Deploy charging-session-service, notification-service, web portal and rebuilt mobile app; configure the values above.
- Verify server-side authentication and per-customer authorization on existing invoice/payment APIs. UI filtering/sign-in alone is not an API security boundary; this change does not retrofit authorization across the existing SaaS APIs. Do not enable payment SMS rollout until this check passes.
- Use provider test settings and a consenting test phone first. Confirm amount, invoice/customer mapping, link after sign-in, QR scan, provider result, repeated stop, and missing-phone/error handling.
- No real SMS, payment, or Azure deployment was performed during implementation.

QR implementation: https://github.com/kazuhikoarase/qrcode-generator/tree/master/js
