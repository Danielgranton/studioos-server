# StudioOS Server

StudioOS Server is the Spring Boot backend for the StudioOS platform. It handles auth, user profiles, studios, bookings, beat marketplace flows, advertisements, payments, notifications, dashboards, and the gRPC bridge to the C++ media service.

## What lives here

- Auth and JWT-based security
- Studio creation, booking, and studio ratings
- Beat marketplace: uploads, purchases, playback, downloads, and reviews
- Advertisement campaigns, ad uploads, media processing orchestration, and review workflow
- Payments, wallets, withdrawals, notifications, and dashboards
- Internal gRPC callback listener for media job completion

## Tech stack

- Java 21
- Spring Boot 3.5.x
- Spring Data JPA + Flyway
- PostgreSQL
- gRPC client for the media service
- S3-compatible object storage for presigned uploads/downloads
- M-Pesa, SMTP, and Africa's Talking integrations

## Requirements

- Java 21
- PostgreSQL
- Maven wrapper (`./mvnw`)
- A running `studioos-media` service if you want real media processing

## Configuration

The server reads configuration from `src/main/resources/application.yaml` and optionally from a local `.env` file.

Common environment variables:

- `DB_HOST`
- `DB_PORT`
- `DB_NAME`
- `DB_USER`
- `DB_PASSWORD`
- `JWT_SECRET`
- `INTERNAL_SERVICE_API_KEY`
- `MEDIA_SERVICE_HOST` default: `localhost`
- `MEDIA_SERVICE_PORT` default: `50051`
- `MEDIA_CALLBACK_GRPC_ENABLED` default: `true`
- `MEDIA_CALLBACK_GRPC_HOST` default: `0.0.0.0`
- `MEDIA_CALLBACK_GRPC_PORT` default: `50052`
- `S3_ENABLED`
- `AWS_REGION`
- `AWS_ACCESS_KEY`
- `AWS_SECRET_KEY`
- `AWS_BUCKET_NAME`
- `AWS_S3_ENDPOINT_URL`
- `AWS_S3_PATH_STYLE`
- `MPESA_*`
- `AFRICASTALKING_*`
- `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`

## Run locally

```bash
export $(cat .env | xargs)
./mvnw spring-boot:run
```

The API runs at `http://localhost:8080/api` by default.

## Run with gRPC media enabled

The production path uses the real gRPC client in the `grpc-enabled` profile.

```bash
export $(cat .env | xargs)
./mvnw spring-boot:run -Dspring-boot.run.profiles=grpc-enabled
```

The server will:

- call the media service at `MEDIA_SERVICE_HOST:MEDIA_SERVICE_PORT`
- expose the callback listener on `MEDIA_CALLBACK_GRPC_PORT`

## Build and test

```bash
./mvnw clean compile
./mvnw test
```

## Database and migrations

Flyway manages the schema in `src/main/resources/db/migration`.

Rules:

- do not edit an applied migration file
- add a new `V{n}__*.sql` file for each schema change
- use `./mvnw flyway:repair` only if you intentionally need to repair a broken checksum history

## Main modules

- `auth` - login, JWT, security filters
- `user` - profiles and identity
- `studio` - studios, studio ratings, producer dashboard inputs
- `booking` - bookings, booking payments, notifications
- `beatmarketplace` - beats, licenses, purchases, downloads, reviews
- `advertisement` - campaigns, ad uploads, pricing, review flow, delivery
- `payment` - transactions, wallets, withdrawals, escrow-like flows
- `notification` - email/SMS/in-app notifications
- `shared/media` - gRPC client, callback server, and polling orchestrator for media jobs

## Media contract

The server talks to the C++ media service with gRPC only.

- submit jobs to `MediaService.SubmitMediaJob`
- poll jobs with `MediaService.GetMediaJob`
- accept callbacks on `MediaCallbackService.ReportMediaJob`

The callback path is a fast path. Polling is the recovery path.

## Security notes

- Never commit `.env`
- Rotate any leaked credentials immediately
- PostgreSQL schema validation is enabled; fix schema drift with migrations, not by disabling validation

## Status

The backend is actively developed. Core marketplace, booking, payments, notifications, gRPC media orchestration, and ad review flows are implemented. Frontend integration and production deployment wiring still need to be finished separately.
