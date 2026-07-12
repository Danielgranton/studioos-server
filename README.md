# StudioOS Server

StudioOS is a Kenyan music production marketplace platform that connects **producers** and **clients**, handling studio bookings, beat purchases, payments, and commissions.

This repository (`studioos-server`) is a **Spring Boot monolith** — a deliberate rewrite from an earlier microservices architecture. Where the old design used inter-service events, this monolith uses internal `ApplicationEventPublisher` / `@EventListener` patterns to keep module boundaries clean without the operational overhead of separate services.

---
---

## Module Status

| Module | Status | Notes |

|---|---|---|
| Auth & core infra | Done | |
| User Profile | Done | Profile images stored as URL strings; AWS S3 upload deferred |
| Studio | Done | Resolved a Hibernate JPQL null-parameter issue via in-memory streaming |
| Booking | Done | Status flow driven by `BookingStatus` enum |
| Notification | Done | Email + SMS for OTP (no DB persistence); Email + SMS + DB for general system notifications |
| Payment | In progress | Wallets, escrow, transactions, withdrawals — M-Pesa STK Push / B2C integration |

**Deferred:** AWS S3 profile image upload integration.

---

---

Commission is calculated as a **rate lookup**, not a hardcoded enum, so rates can vary (e.g. by studio tier) without a code change.

Beat purchases follow a separate, simpler flow tracked via `BeatPaymentStatus` rather than the booking escrow lifecycle.

---

## Getting Started

### Prerequisites

- Java 21
- PostgreSQL (local instance, e.g. `sosdb`)
- Maven wrapper (bundled — no separate Maven install needed)

### Running the server

```bash
export $(cat .env | xargs) && ./mvnw spring-boot:run
```

Or, if the `studioos` alias is defined in `~/.zshrc`:

```bash
studioos
```

> **Note:** this project uses **zsh**, not bash — shell aliases and exported env vars must live in `~/.zshrc`.

The app starts on `http://localhost:8080/api` by default (Tomcat + `/api` context path).

### Media service

When the Java server is run with the `grpc-enabled` profile, it connects to the C++ media service on `localhost:50051` by default.

```bash
export $(cat .env | xargs) && ./mvnw spring-boot:run -Dspring-boot.run.profiles=grpc-enabled
```

Override `MEDIA_SERVICE_HOST` or `MEDIA_SERVICE_PORT` only if the media server runs elsewhere.

---

## Database Migrations (Flyway)

Migrations live in `src/main/resources/db/migration`, following standard `V{n}__description.sql` naming.

**Rules of thumb learned the hard way:**

- Never edit an already-applied migration file — Flyway checksums it, and editing it after the fact causes a
 `FlywayValidateException: Migration checksum mismatch`.
 Always add a new `V{n+1}__*.sql` migration instead.
- If a checksum mismatch does occur (e.g. from an accidental edit), resolve it with:

  ```bash
  export $(cat .env | xargs)
  ./mvnw flyway:repair
  ```

- Numbering conflicts between migrations have occurred during development; resolving them required removing the conflicting row from `flyway_schema_history` directly (version compared as a string cast).

---

## Development Notes

- **Design before code:** for non-trivial modules (like Payment), the architecture — entities, enums, flows — is reviewed and agreed on *before* any code generation begins.
- **Sequential module delivery:** each module is built and verified working before starting the next.
- **Old microservices docs** are kept as a reference for domain logic but are always reconciled against the current monolith structure before being used to guide implementation — the event patterns in particular don't translate 1:1.

---

## Security

- `.env` must never be committed. If it's ever accidentally tracked, rotate any exposed credentials immediately and remove it from git tracking (`git rm --cached .env`) even though `.gitignore` will stop future commits.
- JWT-based auth guards the API; see `auth/JwtAuthFilter`.

---

## Roadmap

- [ ] Finish Payment module (M-Pesa STK Push + B2C, escrow release/refund logic)
- [ ] AWS S3 integration for profile image uploads
- [ ] *(Add your own near-term items here)*

---

## License

MIT
