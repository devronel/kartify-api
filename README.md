# Kartify API

Spring Boot REST API for Kartify, a single-brand Philippine e-commerce store.

## Features

- Public product browsing with search, category filters, featured categories, and new arrivals.
- Session-based auth with CSRF cookies, registration, login, password reset, and email verification checks.
- Cart and checkout flow with order creation, payment retries, and customer order history.
- QR Ph payments through PayMongo, including signed webhook handling.
- Admin endpoints for products, categories, and orders.
- MySQL with Flyway migrations, JPA validation, ImageKit/local image storage, and Mailtrap/Thymeleaf email.

## Installation

Prerequisites: Java 25, MySQL, and the included Maven wrapper.

```bash
cp .env.example .env
# Fill in .env with your database, mail, ImageKit, storage, PayMongo, and app URL values.
./mvnw spring-boot:run
```

Flyway runs when the app starts. Create the database referenced by `DB_URL` first.

Run tests or build a package:

```bash
./mvnw test
./mvnw clean package
```

## Usage

Browse products:

```bash
curl "http://localhost:8080/api/products?page=1&pageSize=10"
```

Log in:

```bash
curl -c cookies.txt -X POST http://localhost:8080/api/authenticate \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"your-password"}'
```

For state-changing requests, call `GET /api/csrf-cookie`, then send the CSRF token in the `X-XSRF-TOKEN` header with the session cookie.

## Configuration

Environment variables are defined in `.env.example`:

- `URL_STOREFRONT_SITE`, `URL_ADMIN_SITE`, `APP_URL`
- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `DB_DIALECT`
- `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`
- `IMAGEKIT_PRIVATE_KEY`, `IMAGEKIT_PUBLIC_KEY`, `IMAGEKIT_URL`
- `STORAGE_DIRECTORY`
- `PAYMONGO_SECRET_KEY`, `PAYMONGO_PUBLIC_KEY`, `PAYMONGO_WEBHOOK_SECRET`

For local PayMongo webhooks, expose the Spring port with a public tunnel and register `/api/webhooks/paymongo` in the PayMongo dashboard.