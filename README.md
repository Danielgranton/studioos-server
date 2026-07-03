# StudioOS Server

StudioOS is a Kenyan music production marketplace platform that connects **producers** and **clients**, handling studio bookings, beat purchases, payments, and commissions.

This repository (`studioos-server`) is a **Spring Boot monolith** — a deliberate rewrite from an earlier microservices architecture. Where the old design used inter-service events, this monolith uses internal `ApplicationEventPublisher` / `@EventListener` patterns to keep module boundaries clean without the operational overhead of separate services.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language / Runtime | Java 21, Spring Boot 3.5.16 |
| Web | Spring Web (REST), Spring WebFlux (for M-Pesa HTTP calls via WebClient) |
| Realtime | Spring WebSocket (chat, calls, notifications) |
| Persistence | Spring Data JPA + Hibernate, PostgreSQL |
| Migrations | Flyway |
| Auth | Spring Security + JWT (`io.jsonwebtoken` / jjwt) |
| Payments | M-Pesa STK Push (inbound), M-Pesa B2C (withdrawals) |
| Build | Maven (`./mvnw`) |
| Dev Env | Kali Linux, zsh |

---

## Project Structure

Base package: `com.studioos.server`

```
com.studioos.server
├── auth/               # JWT filters, authentication
├── shared/
│   └── enums/          # Shared domain enums (see below)
├── user/                # User profiles
├── studio/              # Studio listings
├── booking/              # Studio booking lifecycle
├── payment/               # Transactions, wallets, escrow, withdrawals
└── notification/           # Email + SMS + in-app notifications
```

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

## Domain Enums

Enum discipline matters here — always verify against `shared/enums` before writing new code, since names/values have shifted during development.

```
TransactionStatus       PENDING, SUCCESS, FAILED
TransactionType          DEPOSIT, BOOKING_PAYMENT, BEAT_PURCHASE, COMMISSION, WITHDRAWAL, REFUND
WithdrawalStatus         PENDING, APPROVED, REJECTED, COMPLETED
EscrowStatus              HELD, RELEASED, REFUNDED
BookingStatus              PENDING, APPROVED, RECORDING, MIXING, READY, DELIVERED, CANCELLED
BookingPaymentStatus        BOOKED, PAID
BeatPaymentStatus            PENDING, PAID, FAILED
AuditEventType                 Transaction, wallet, escrow, and top-up audit events
```

---

## Payment Module (Architecture Summary)

The Payment module is built around five core entities:

- **Transaction** — the ledger record for every money movement (deposit, booking payment, beat purchase, commission, withdrawal, refund). Tracks M-Pesa identifiers: `mpesaCheckoutRequestId`, `mpesaMerchantRequestId`, `mpesaReceiptNumber`, `mpesaPhoneNumber`.
- **Wallet** — producer/studio balance, split into `availableBalance`, `pendingBalance`, and `withdrawnBalance`.
- **Escrow** — holds booking funds until the booking reaches `DELIVERED` (release) or `CANCELLED` (refund).
- **Withdrawal** — producer payout requests, settled via M-Pesa B2C.
- **AuditLog** — immutable record of transaction, wallet, escrow, and top-up events.

**Booking payment flow:**

```
Client initiates M-Pesa STK Push
        │
        ▼
Transaction (PENDING) created
        │
        ▼
M-Pesa callback → SUCCESS / FAILED
        │
        ▼
On SUCCESS → funds held in Escrow (HELD)
        │
        ├── Booking reaches DELIVERED → Escrow RELEASED → Wallet credited
        └── Booking CANCELLED           → Escrow REFUNDED → client refunded
```

Commission is calculated as a **rate lookup**, not a hardcoded enum, so rates can vary (e.g. by studio tier) without a code change.

Beat purchases follow a separate, simpler flow tracked via `BeatPaymentStatus` rather than the booking escrow lifecycle.

---

## Getting Started

### Prerequisites

- Java 21
- PostgreSQL (local instance, e.g. `sosdb`)
- Maven wrapper (bundled — no separate Maven install needed)

### Environment Variables

Copy `.env.example` to `.env` and fill in real values. **Never commit `.env`.**

```
DB_HOST=localhost
DB_PORT=5432
DB_NAME=sosdb
DB_USER=postgres
DB_PASSWORD=your_local_password

MPESA_PASSKEY=your_mpesa_passkey
MPESA_ENVIRONMENT=sandbox
# MPESA_INITIATOR_PASSWORD=...
```

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
