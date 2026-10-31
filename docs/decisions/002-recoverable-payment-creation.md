# 002 - Make Payment Creation Recoverable

- **Status:** Proposed
- **Date:** 2026-10-31

## 1. Context

Currently, Kartify processes checkout in the following order:

1. Create the Order.
2. Request a Payment Link from PayMongo.
3. Save the Payment information locally.

The problem occurs when PayMongo successfully creates a Payment Link, but saving the payment information in the local database fails.

This leaves Kartify with an existing Order and a Payment Link on PayMongo, but without a complete local Payment record.

Another problem occurs when PayMongo fails to create the Payment Link. The Order has already been created, and the customer has no checkout URL.

## 2. Decision

Change the execution order to create the local Order and Payment records before communicating with PayMongo.

The proposed flow is:

1. Create the Order.
2. Create a local Payment record with `CREATING` status.
3. Commit the database transaction.
4. Request a Payment Link from PayMongo.
5. Save the PayMongo Payment Link ID and checkout URL to the existing Payment record.
6. Update the Payment status to `PENDING`.
7. Return the checkout URL to the frontend.

The Order and Payment records should be created within the same local database transaction.

## 3. Alternatives Considered

### Option A: Keep the Current Execution Order

Create the Order, request a Payment Link, and then save the Payment locally.

**Advantages:**
- Simple implementation.
- Payment information is saved after receiving a successful PayMongo response.

**Disadvantages:**
- A failed local database operation can leave an orphaned Payment Link.
- An Order can exist without a corresponding local Payment record.
- Recovery becomes more difficult when the PayMongo response is lost.

### Option B: Create Local Payment Before Calling PayMongo

Create the Order and Payment records first, then request the Payment Link and update the existing Payment record.

**Advantages:**
- Every successfully committed Order has a corresponding local Payment record.
- Existing Payment records can be used for recovery.
- Payment creation can be retried without creating another Order.

**Disadvantages:**
- Requires additional payment creation statuses.
- Requires retry and recovery mechanisms.
- A successful PayMongo request can still leave the local Payment record incomplete if the database update fails.

## 4. Reasoning

Choose Option B because it provides a persistent local record before communicating with the external payment provider.

This makes it easier to identify incomplete payment creation attempts and recover them without repeating checkout.

Since PayMongo is an external service, its operations cannot be rolled back using a local database transaction.

## 5. Consequences

### Positive
- Order and Payment records are created together.
- Reduces the possibility of Orders without local Payment records.
- Existing Payment records can be used for recovery.
- Checkout becomes easier to retry without recreating the entire Order.

### Negative
- Payment records can remain in `CREATING` when PayMongo or the local database operation fails.
- Requires a mechanism to recover incomplete payment creation attempts.
- Requires protection against duplicate Payment Links.
- Webhook processing must reliably identify the corresponding local Payment.

## 6. Follow-up Improvements

- [x] Implement the new checkout execution order.
- [x] Add payment creation status management.
- [ ] Implement safe retry handling.
- [ ] Investigate PayMongo idempotency support.
- [ ] Ensure webhook events can identify the correct local Payment.
- [ ] Implement recovery for incomplete payment creation attempts.
- [ ] Add tests for PayMongo failures and database failures.

## 7. References

- [PayMongo API Documentation](https://developers.paymongo.com/)
- Related classes: `OrderService`, `PaymentService`, `PaymongoService`.
- Related concepts: Database Transactions, Idempotency, Retry Mechanisms, Webhooks, Reconciliation.