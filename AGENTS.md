# AGENTS.md — Kartify API (backend)

Spring Boot REST API for Kartify, a single-brand Philippine e-commerce store (personal learning project, built deploy-ready). The Next.js storefront and admin are in a separate repo and consume this API.

## Stack

- Java 25, Spring Boot, Maven
- Spring Security (cookie session + CSRF cookie, not JWT), Spring Data JPA / Hibernate
- MySQL, Flyway migrations in `src/main/resources/db/migration`
- PayMongo, Mailtrap + Thymeleaf for email
- Base package: `com.kartify.api`

## Commands

- Run: `./mvnw spring-boot:run`
- Test all: `./mvnw test`
- Test one class: `./mvnw test -Dtest=OrderServiceTest`
- Build: `./mvnw clean package`
- Secrets come from environment variables (`PAYMONGO_SECRET_KEY`, `PAYMONGO_WEBHOOK_SECRET`). Never hardcode or commit them.
- Local webhooks need a public tunnel (ngrok or VS Code port forwarding) pointing at the Spring port, registered in the PayMongo dashboard as `/api/webhooks/paymongo`.

## Before finishing any task

- Run `./mvnw test`. Add tests for new service logic, including edge cases (duplicates, boundaries, wrong owner).
- Test time-dependent rules with a fixed `now` passed in, never with sleeps.
- Do not add features outside the request. Deferred on purpose: coupons, reviews, wishlists, multi-vendor, roles table, carrier shipping, partial checkout, GCash/Maya, PayPal.

## Package layout (where new code goes)

Feature-based. Inside a feature, follow the sub-packages already there (`controller`, `service`, `repository`, `entity`, `dto`, `enums`).

- `auth`, `account`, `category`, `cart`, `order`, `payment`, `product` (attributes, variants, images live inside it), `user` (entities and repositories only)
- `order` owns `Order`, `OrderItem`, `OrderStatusHistory`, `Payment`, the order/payment enums, `OrderService`, `PaymentService`, `CheckoutService`, and the webhook controller.
- `payment` is a thin PayMongo adapter: it takes and returns plain values and never touches `Order` or `Payment` entities. Dependencies run `order → payment`, `order → cart`, never the reverse.
- Logic about an entity goes in that entity's own service. No `*Helper` or `*Utils` classes.
- Controllers parse input, call one service method, and return a DTO. No business logic.
- Never return entities. DTOs are named by consumer and match the existing naming (for example `OrderCreateRequest`). Keep public, customer, and admin responses separate (`PublicProductResponse` has no `costPrice`).

## API conventions

- `POST` that creates returns 201. `PUT` replaces, `PATCH` changes part, `DELETE` is used even for soft deletes.
- IDs come from the path, never the body. The user comes from the session (`CustomUserDetails.getId()`), never the request.
- Ownership uses scoped queries (`findByIdAndUserId`, `findByOrderNumberAndUserId`, `findByIdAndCartUserId`). Wrong owner returns 404, not 403.
- Errors go through `GlobalExceptionHandler` with one shape: `{ timestamp, status, message, errors }`.
  - `FieldValidationException(field, message)` → 422, `errors: { field: message }`
  - `CartItemsUnavailableException` → 422, `errors: { items: [...] }`
  - `ResourceNotFoundException` → 404
  - The catch-all handler logs with the stack trace and never returns `ex.getMessage()`.
- Never catch exceptions in controllers to build responses.
- Order lookups by the public order number (`ORD-<year>-<12 hex>`), never expose internal IDs, `provider_payment_id`, payment `payload`, or `checkout_url` unless the response is designed to.

## Money and time

- Money is `BigDecimal` (`DECIMAL(10,2)` in MySQL), never `double`. Compare with `compareTo`, not `equals`.
- PayMongo amounts are centavos. Convert only inside `PaymongoService` with `movePointRight(2).intValueExact()`, and back with `BigDecimal.valueOf(centavos, 2)`.
- Event moments from other systems use `Instant` (`paidAt`). Existing created/updated fields may be `LocalDateTime`. Keep each comparison within one type.
- Domain time rules take `now` and the hold duration as parameters. `PENDING_HOLD` (30 minutes) in `OrderService` is the single source for order expiry, shared by responses, the pay endpoint, and the expiry job.

## Database and JPA

- One Flyway file per table, in dependency order. MySQL does not roll back DDL.
- Never edit a migration that has run. Add a new `ALTER TABLE` migration.
- Constraint names are unique across the whole schema, with a per-table prefix (`fk_order_items_order`, `fk_osh_changed_by`). Never copy a constraint name from another file.
- Enums are `VARCHAR` columns with `@Enumerated(EnumType.STRING)`, never MySQL `ENUM`. Rules about an enum live in the enum (`canTransitionTo`, `customerAction`). Switch expressions over enums have no `default`, so a new value fails to compile until handled.
- Entities need a no-arg constructor. Use `cascade = ALL` with `orphanRemoval` only for owned children: `User→UserDetail`, `Cart→CartItem`, `Order→OrderItem`, `Order→OrderStatusHistory`. Never for `User→Cart`, `Category→children`, `User→UserAddress`.
- Products are soft-deleted (`@SQLDelete` + `@SQLRestriction`). `OrderItem` keeps `productId` and `productVariantId` as plain `Long` fields, not `@ManyToOne`, so a soft-deleted product can't break order loading.
- Order data is a snapshot (address in `shipping_*` columns, item name/price/image in `order_items`). Order responses read only snapshots, never live products.
- No `clearAutomatically = true` on `@Modifying` queries (it drops unsaved work in the same transaction).
- Orders are never deleted with a user (no cascade from `users`).

## Transactions and concurrency

- Never call an external API (PayMongo) inside `@Transactional`. `CheckoutService` is not transactional: it calls `OrderService.create` (commits), then PayMongo, then a separate short transaction saves the result. Calls to another method on the same class bypass `@Transactional`, so use separate beans.
- Stock changes only through atomic conditional updates (`SET stock = stock - :qty WHERE id = :id AND stock >= :qty`). Check the affected row count: 0 means lost the race, so throw and roll back. Apply updates in a stable order (simple products first, then variants, ascending ID) to avoid deadlocks.
- Variant products use the variant's stock. The product's `stock_quantity` is ignored when `hasVariants = true`.
- Order creation locks the cart row (`findByUserIdForUpdate`). Webhook, pay, and expiry code lock the order row before checking status.
- Checkout re-validates the cart with `CartService.findProblems`, the same check the cart page uses. Never duplicate it.
- Stock is taken at order creation and restored when a `PENDING` order is cancelled or expires.

## Orders and payments

- Order statuses: `PENDING → PAID | CANCELLED`, `PAID → PROCESSING | CANCELLED`, `PROCESSING → SHIPPED | CANCELLED`, `SHIPPED → DELIVERED`. `DELIVERED` and `CANCELLED` are final.
- Every order status change goes through one `changeStatus(order, next, notes, changedBy)` method: validates the transition, updates `orders.status`, and appends an `order_status_histories` row in the same transaction. Never call `setStatus` directly. History is append-only. `changed_by` is null for system events.
- Do not add a `payment_status` column to `orders`. Read it from the latest payment row.
- A payment attempt is always a new `payments` row. Never reuse or edit a finished one. At most one open attempt per order.
- Each attempt has its own idempotency key, sent to PayMongo as `Idempotency-Key`. Reuse the same key only when the last outcome is unknown (timeout, no response). A new attempt gets a new key.
- Payment is online only, through QR Ph. The PayMongo link does not redirect back to the site and does not expire by itself, so the order page polls for status and the expiry job must archive the link.
- The webhook is the only authority that an order is paid. Verify `Paymongo-Signature` first, using the raw body (`@RequestBody String`), before any parsing. Act only on `link.payment.paid`, match by link ID, and check the amount. Make it idempotent, and return 200 for unknown or ignored events. A payment for an already-cancelled order is saved as `SUCCEEDED`, the order stays `CANCELLED`, and it is logged for a manual refund.
- The webhook path is public and CSRF-exempt. Every other state-changing endpoint needs the CSRF token. `/api/admin/**` requires `ADMIN`.

## Logging

- Use SLF4J. Expected business outcomes (blocked checkout, unknown webhook payment) are `WARN` or `INFO`. Real failures are `ERROR` with the exception as the last argument.
- Never log request bodies, webhook bodies, addresses, phone numbers, or secrets. Log IDs and order numbers.

## Boundaries

**Ask first:** adding dependencies, changing security config, changing a persisted enum value, changing an API response or error shape (the frontend depends on it), deleting files.

**Never:** commit secrets, edit an applied migration, use floating point for money, update `orders.status` outside `changeStatus`, call PayMongo inside a transaction, return entities, or expose `payload` or provider IDs in responses.

## More detail (read only when relevant)

- `docs/01-database-schema.md` — tables, foreign keys, final SQL
- `docs/02-enums-and-status-rules.md` — status rules and transitions
- `docs/03-backend-architecture.md` — package and entity decisions
- `docs/06-checkout-and-order-flow.md` — order creation steps
- `docs/07-payments-paymongo.md` — payment and retry rules
- `docs/09-webhook-signature-verification.md` — signature check
- `docs/08-progress-and-next-steps.md` — what is built and what is pending