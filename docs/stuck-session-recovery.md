# Missing final OCPP transaction event

Remote-stop acceptance is not proof of a physical stop. The web portal first waits for the final event, then requests TriggerMessage(StatusNotification). It does not force a connector available on a timer.

A timestamped Available notification from the authenticated charger reconciles an ACTIVE session on that connector to INTERRUPTED. The report must be newer than the last session update, no more than five minutes old, and not future-dated. Keep charger clocks synchronized. OCPP 2.x connectorStatus and the ocpp2.0 alias are supported.

Reconciliation is row-locked, preserves the last recorded meter, releases the active-connector uniqueness lock and does not queue automatic billing. INTERRUPTED means final meter pending, not normally completed. A late final event can finalize the old session once; it preserves the recovery stop time and must not change the next car's connector status. A lower meter reading is rejected for investigation.

If the charger remains charging, offline, or cannot send fresh status, recovery deliberately remains blocked. Stop charging at the charger/simulator and unplug before requesting Available. Never manufacture Available status on a charger still supplying power.

## Configuration and deployment

SESSION_RECOVERY_KEY must be the same nonempty private random secret on charging-session-service and ocpp-gateway. The recovery endpoint denies missing/incorrect keys and must not be called by browsers. This is separate from the charger shared key. The Azure payment-notifications deployment helper provisions/reuses this secret without printing it. Restart both services after setting it locally. Do not commit its value.

Deploy charging-session-service and ocpp-gateway before the updated web portal using tools/deploy-payment-notifications-azure.ps1. Missing key disables automatic reconciliation safely. No database schema migration is required: status is a VARCHAR and INTERRUPTED uses the existing fields.

## Verification

1. Start one simulator transaction and send meter values.
2. Stop from the portal; verify RequestStopTransaction (2.x) or RemoteStopTransaction (1.6).
3. Withhold the final event but send a fresh Available status. Confirm INTERRUPTED, unchanged last meter, no automatic invoice and connector Available.
4. Start the next car on that connector.
5. Deliver the first car's final event. Confirm first session COMPLETED and a single billing job, while the next session remains ACTIVE and its connector unchanged.
6. Repeat final events; ensure no duplicate bill. Test old/future Available timestamps and missing internal keys: no recovery allowed.

Local regression tests cover backend reconciliation/authorization/database locks, protocol status mapping, late final-event isolation and frontend polling. Live Azure/simulator verification is still required after deployment.
