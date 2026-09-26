# Automatic charging-session billing

New sessions stopped with COMPLETED status queue billing in the same database transaction as the final meter reading. A background worker runs every five seconds. It creates one bill per session, creates one invoice per bill, and issues that invoice. The customer web portal displays a payment-due notice and links to Payments → Invoices; invoice refresh runs every ten seconds while the customer is signed in.

No money is collected automatically. Razorpay must be enabled and configured for the workspace. The customer explicitly chooses Pay online and completes checkout. Optional SMS, mobile inbox and QR payment links are documented in [payment-notifications.md](payment-notifications.md); native Android checkout is not included.

Zero-cost sessions retain a bill but do not issue a payment-request invoice.

## Reliability

- Queue state is persistent; a service restart does not lose pending billing.
- A two-minute database lease prevents multiple replicas processing the same session simultaneously and allows recovery from crashes.
- Retries look up existing bills by session and invoices by bill. Existing unique database constraints prevent duplicate records when creation races or responses are lost.
- Draft invoices are issued on retry; issued/paid/void invoices are not reissued.
- Repeated stop requests with identical final values are acknowledged without changing timestamps or queueing again. Conflicting final readings are rejected.
- Failed/cancelled sessions and historical completed sessions are not automatically charged or back-billed.
- Pending work is logged with the session ID, without customer details or credentials.

## Deployment

Deploy the charging-session-service and rebuilt admin portal. Flyway applies V4 automatically; do not manually edit historical session data.

The charging-session-service must have these environment variables in Azure:

```text
BILLING_SERVICE_URL=http://billing-service
INVOICE_SERVICE_URL=http://invoice-service
USER_SERVICE_URL=http://user-service
AUTO_SESSION_BILLING_ENABLED=true
```

Keep at least one charging-session-service replica running so queued work continues without incoming requests. The Azure Bicep configuration now specifies always-on for this service (may increase Azure costs). Local defaults use ports 8090, 8091 and 8082. Set AUTO_SESSION_BILLING_ENABLED=false to pause processing without discarding queued work.

## Verification

1. Configure a tariff and an active customer RFID card. Configure Razorpay test credentials for payment testing.
2. Start a new simulator session and send meter readings, then TransactionEvent Ended / StopTransaction.
3. Confirm the session is COMPLETED. Within normal service response times, one bill and one ISSUED invoice should appear.
4. Sign in as the RFID-card customer: the payment notice should link to their invoice. Payment remains pending until checkout succeeds.
5. Repeat the same stop message. Confirm no duplicate bill, invoice, or changed final reading.
6. Stop a billing dependency, complete another session, restore the dependency, and allow two minutes for retry. Confirm the same single bill/invoice is used.

These instructions do not imply an Azure deployment or a real payment was performed.
