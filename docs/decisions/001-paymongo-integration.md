# PayMongo Integration

## Context

Kartify is an e-commerce application that needs to support online payments during checkout.

## Initial Decision

PayMongo was my initial choice because it was the first payment provider that came to mind when I started implementing the payment feature.

I did not initially compare other payment providers or evaluate alternative payment approaches.

## Implementation

Kartify uses PayMongo Payment Links to provide a hosted checkout experience.

The application:
- Creates a local order and payment record.
- Generates a PayMongo Payment Link.
- Redirects the customer to PayMongo's hosted checkout page.
- Receives payment notifications through a webhook.
- Verifies the webhook signature and payment amount.
- Updates the local payment and order status after successful payment.

## Current Limitations

Order creation and Payment Link creation are separate operations.

If Payment Link creation fails after the order has been created, the application may leave an order without a usable payment link, while stock has already been deducted and the cart cleared.

## Lessons Learned

- Understand the difference between a local payment record and a payment provider's records.
- Understand how webhooks are used to confirm payments.
- Consider transactional consistency when integrating external services.
- Evaluate alternative payment providers before making future integration decisions.