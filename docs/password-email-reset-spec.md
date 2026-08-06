# Email Template — Password Reset

## Context

- **Backend:** Spring Boot, sending via Mailtrap (SMTP sandbox, test mode)
- **Templating approach:** Thymeleaf HTML email template (standard Spring Boot approach — `src/main/resources/templates/email/password-reset-email.html`), rendered server-side and sent as HTML email via `JavaMailSender`
- **Purpose:** Email sent when a user requests a password reset, containing a link to set a new password
- **Brand name:** Kartify (placeholder — swap if a final name/niche is chosen)

## Email Requirements

### General Email Design Constraints (important — email HTML is not regular HTML)

- Use **table-based layout**, not flexbox/grid — many email clients (especially Outlook) don't support modern CSS layout
- Use **inline CSS** on every element — most email clients strip `<style>` blocks or external stylesheets
- Max width: **600px**, centered, for consistent rendering across devices and clients
- Use web-safe fonts only (e.g. Arial, Helvetica, sans-serif fallback stack) — custom fonts often don't render
- Avoid background images for critical content — some clients block them by default
- All images (if any, e.g. logo) need explicit `width`/`height` attributes and `alt` text
- Buttons should be real `<a>` tags styled to look like buttons (padding, background-color, border-radius inline), not `<button>` elements — better cross-client support

### Layout Structure

```
┌─────────────────────────────────────┐
│   [Logo / Brand Name — text is fine] │
├─────────────────────────────────────┤
│                                       │
│   Hi {{firstName}},                  │
│                                       │
│   We received a request to reset     │
│   your password. Click the button    │
│   below to choose a new one.         │
│                                       │
│         [ Reset Password ]           │  ← button, links to reset URL
│                                       │
│   This link will expire in           │
│   {{expiryMinutes}} minutes.         │
│                                       │
│   If you didn't request this, you    │
│   can safely ignore this email —     │
│   your password will not be changed. │
│                                       │
├─────────────────────────────────────┤
│   Having trouble with the button?    │
│   Copy and paste this link:          │
│   {{resetUrl}}                       │
├─────────────────────────────────────┤
│   © {{year}} Kartify. All rights     │
│   reserved.                          │
└─────────────────────────────────────┘
```

### Template Variables (Thymeleaf placeholders)

| Variable | Type | Example | Notes |
|---|---|---|---|
| `firstName` | String | "Juan" | Personalize greeting |
| `resetUrl` | String | `https://kartify.app/reset-password?token=abc123` | Full URL, points to the Next.js reset password page with token as query param |
| `expiryMinutes` | Integer | 60 | Should match the actual backend token expiry logic |
| `year` | Integer | 2026 | For footer copyright, can be set dynamically server-side |

### Copy/Tone Guidelines

- Keep it short — this is a transactional email, not marketing
- Reassuring tone for the "if you didn't request this" line — avoid alarming language
- One clear call-to-action (the button) — don't add unrelated links/promotions

### Security-Related Content Notes (for the backend logic generating this email, not the template itself)

- The reset link's token should be single-use and expire after a fixed window (reflected in `expiryMinutes`)
- Do not include the user's password (obviously) or account details beyond first name in the email
- The "if you didn't request this" line matters — since this email might be triggered by someone else entering the user's email address, not necessarily the account owner

## Plain-Text Fallback

Provide a plain-text version alongside the HTML version (`JavaMailSender` supports multipart messages) for email clients that don't render HTML, and for better spam-filter scoring:

```
Hi {{firstName}},

We received a request to reset your password. Use the link below to choose a new one:

{{resetUrl}}

This link will expire in {{expiryMinutes}} minutes.

If you didn't request this, you can safely ignore this email — your password will not be changed.

— Kartify
```

## Testing Notes

- Since this project uses Mailtrap (sandbox), the email will not actually be delivered to a real inbox — it'll appear in the Mailtrap inbox dashboard for visual/HTML inspection
- Mailtrap's inbox view includes an "HTML check" / spam-score style analysis tool — worth checking after building the template to catch obvious cross-client rendering issues early

## Out of Scope

- Other email templates (welcome/registration, order confirmation) — not part of this spec, to be built separately if/when needed
- Actual token generation/expiry backend logic — this spec covers the email template only, not the reset flow's security implementation