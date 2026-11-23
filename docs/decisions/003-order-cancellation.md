# Customer Can Cancel Orders in PENDING Status Only

- **Status:** Proposed
- **Date:** 2026-11-23

## 1. Context
Kartify has no refund feature. If a customer could cancel an order that is already paid,
they would be left without an order and without their money, and nothing in the system
returns it. Refunds for QR Ph payments are also uncertain: PayMongo's documentation
disagrees on whether they are possible, and the dedicated flow needs the customer to
claim a transfer link.

A PENDING order has no payment taken, so cancelling it needs no refund. Letting customers
cancel it also frees the stock that the unpaid order is holding.

This decision covers the customer side only. Admin cancel rules are a separate decision.

## 2. Decision
- A customer can cancel an order only while its status is PENDING.
- PAID, PROCESSING, SHIPPED and DELIVERED orders can't be cancelled by the customer.
- The rule is enforced in the backend: the order is loaded by order number and user with a
  row lock, and anything other than PENDING is rejected.
- The UI shows the Cancel button only when the backend says `canCancel` is true. Other
  orders show "Need to change this order? Contact us."
- Cancelling a PENDING order uses the shared cancel method (same as the expiry job): the
  open payment becomes EXPIRED, the PayMongo link is archived after the commit, the stock
  is restored, and a history row is written.

## 3. Alternatives Considered
- **Option A: customer can cancel PENDING and PAID.**
  Advantage: more flexible for the customer.
  Disadvantage: the customer creates refund work, the money stays with the store with no
  way to return it, and "paid but cancelled" becomes a common state.
- **Option B: customer can cancel PENDING, PAID and PROCESSING.**
  Advantage: the most flexibility.
  Disadvantage: staff may already be packing, there is a race with the admin marking it
  shipped, and it creates even more refund cases.
- **Option C: customers can't cancel at all.**
  Advantage: the simplest.
  Disadvantage: a customer who made a mistake can't release the stock their unpaid order
  holds, and has to wait for the 30 minute expiry.

## 4. Reasoning
- No money has been taken at PENDING, so cancelling is safe and needs no refund.
- Without a refund feature, any cancel after payment leaves the customer unpaid back.
- Fulfilment hasn't started, so nobody's work is wasted.
- It reuses the same cancel code as the expiry job, so there is one place that restores
  stock and closes payments.
- It follows my rule of not building features before something needs them.

## 5. Consequences
**Advantages**
- Customer cancels never need a refund.
- The rule is simple to explain, to show in the UI, and to test.
- Customers can't create "paid but cancelled" orders themselves.
- Stock held by an unpaid order is released immediately.

**Disadvantages and limitations**
- A customer who paid and changed their mind must contact support, even before packing.
- The page needs a clear "contact us" message for orders that can't be cancelled.
- A race is possible between the customer cancelling and the payment webhook:
  - If the webhook wins, the cancel is rejected and the page refetches and shows the order as paid.
  - If the cancel wins and the customer then pays the old link, it is a late payment: the payment is saved as SUCCEEDED, the order stays CANCELLED, and it needs a manual refund. Archiving the link after the commit narrows this window.

**Implementation notes**
- The customer lookup is scoped to the user, so another user's order number returns 404.
- The customer order response has a `canCancel` flag, true only for PENDING.
- Error for a non-PENDING order: "This order can't be cancelled anymore. Please contact us."
- `canTransitionTo` still allows PAID to CANCELLED. The restriction is in the cancel action, not the status rules, so it can change later without touching them.

## 6. Notes / References
- Admin cancel rules: see the separate decision for admin.
- Revisit when the refund feature is built.
- PayMongo QR Ph refunds: https://docs.paymongo.com/docs/qrph-refunds
- PayMongo refunds overview (says QR Ph can't be refunded): https://docs.paymongo.com/docs/payment-acceptance-refunds
- Related: the expiry job and the shared cancel method, `OrderStatus.canTransitionTo`.