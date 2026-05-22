# KOINS Loan & Wallet Backend

A production-grade REST API for loan origination, wallet management, and payment processing. Built with Spring Boot 3.3 and Java 17, it handles the full loan lifecycle — from application and OTP-verified signup through amortization scheduling, disbursement, repayment, and automated overdue detection.

---

## Table of Contents

- [Tech Stack](#tech-stack)
- [Architecture](#architecture)
- [Key Features](#key-features)
- [Getting Started](#getting-started)
- [Environment Variables](#environment-variables)
- [API Reference](#api-reference)
- [API Documentation (Swagger)](#api-documentation-swagger)
- [Testing](#testing)

---

## Tech Stack

| Layer | Technology |
|---|---|
| Framework | Spring Boot 3.3 (Java 17) |
| Persistence | Spring Data JPA + PostgreSQL 15 |
| Caching / Session | Redis (token blocklist, OTP store) |
| Security | Spring Security + JJWT 0.12.5 (HS256) |
| Build | Maven Wrapper (`./mvnw`) |
| API Docs | SpringDoc OpenAPI 2.5 / Swagger UI |
| Boilerplate Reduction | Lombok 1.18.46 |
| Containerisation | Docker Compose |

---

## Architecture

The codebase enforces a strict unidirectional dependency chain:

```
Controller  →  Service  →  Repository  →  Entity (Domain)
```

No layer may skip another. Controllers delegate immediately to services; services own all business logic; repositories are pure Spring Data JPA interfaces; domain entities carry no business logic.

### Package Layout

```
com.koins.loanbackend/
├── controller/          # REST endpoints — thin delegation only
├── service/             # All business rules live here
├── repository/          # Spring Data JPA interfaces
├── domain/              # JPA entities + enums
│   └── enums/
├── dto/
│   ├── request/         # Inbound payloads (validated)
│   ├── response/        # Outbound envelopes (never expose entities)
│   └── webhook/         # Paystack payload shapes
├── security/            # JWT filter, token provider, blocklist, SecurityConfig
├── config/              # OpenAPI / Swagger configuration
├── exception/           # GlobalExceptionHandler + ApiErrorResponse
└── service/             # Schedulers, seeders, amortization engine
```

---

## Key Features

### JWT Authentication with Stateless Logout
Tokens are signed with HMAC-SHA256. Logout is implemented without server-side session state: the token is written to a Redis key (`jwt:blocklist:{token}`) with a TTL equal to the token's remaining lifetime. The `JwtAuthenticationFilter` rejects any request bearing a blocklisted token. Redis entries auto-expire — the blocklist never grows unbounded.

### Wallet Auto-Creation & Pessimistic Locking
A wallet is created in the same database transaction as user signup. Balance invariants are enforced at the service layer: every balance update acquires a `PESSIMISTIC_WRITE` lock on the wallet row, preventing race conditions under concurrent requests. The balance may never go below zero — a domain exception is thrown before any `save()` call.

### Loan Lifecycle & Amortization Engine
Loans follow a strict state machine: `PENDING → APPROVED → DISBURSED → CLOSED / DEFAULTED`.

- **Eligibility check:** requested amount ≤ 3× the applicant's current wallet balance.
- **Amortization:** two methods are supported, selected at approval time:
  - **Reducing Balance (EMI)** — equal monthly instalments where interest is calculated on the outstanding principal each month.
  - **Flat Rate** — interest is calculated on the original principal for every instalment.
- **Repayment schedule:** generated atomically on approval and stored as a `jsonb` column in PostgreSQL via a custom JPA `@Converter`. Each instalment tracks its own `status`, `dueDate`, and `lateFee`.
- **Disbursement:** credits the borrower's wallet and records a `DISBURSEMENT` transaction.
- **Repayment:** debits the wallet, marks the earliest `UNPAID` instalment as `PAID`, and records a `REPAYMENT` transaction.

### Transaction Audit Trail
Every credit, debit, disbursement, and repayment operation persists a `Transaction` record before returning. This rule is enforced at the service layer — no financial operation completes without an audit entry.

### Idempotent Operations
The `POST /wallets/fund`, `POST /loans/{id}/repay`, and `POST /loans/{id}/disburse` endpoints all require an `Idempotency-Key` request header. Duplicate keys within the TTL window return the original response without re-executing the operation — safe for client retries.

### Paystack Webhook Integration
The `/api/v1/webhooks/paystack` endpoint verifies the `x-paystack-signature` header using HMAC-SHA512 before processing any event. Idempotency is enforced via a `WebhookEvent` table: duplicate `charge.success` events for the same payment reference are silently deduplicated without double-crediting wallets.

### Automated Overdue & Default Detection
A scheduled job (`LoanScheduler`) runs daily to:
1. Identify instalments whose `dueDate` has passed while still `UNPAID`.
2. Apply a configurable late fee (default 2% of the instalment).
3. Transition the instalment to `OVERDUE`.
4. Mark the parent loan as `DEFAULTED` if the consecutive overdue count exceeds the configured threshold (default 3).

### OTP-Based Account Activation
New user accounts start in a `PENDING` state. A one-time passcode is issued at registration and verified before the account becomes `ACTIVE` and the wallet is created.

### Role-Based Access Control
Two roles: `USER` and `ADMIN`. Loan approval and disbursement endpoints are restricted to `ADMIN` via `@PreAuthorize("hasRole('ADMIN')")`. An admin account is seeded automatically on startup from environment-variable credentials.

### Uniform Error Responses
`GlobalExceptionHandler` (`@RestControllerAdvice`) maps every exception to a consistent JSON envelope:

```json
{
  "timestamp": "2025-10-01T14:32:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Loan amount must not exceed 3× wallet balance",
  "path": "/api/v1/loans"
}
```

---

## Getting Started

### Prerequisites

- Docker & Docker Compose
- Java 17+ (the project compiles to Java 17 bytecode)
- Maven Wrapper is bundled — no separate Maven installation required

### 1. Clone the repository

```bash
git clone https://github.com/your-org/koins-loan-backend.git
cd koins-loan-backend
```

### 2. Start infrastructure services

```bash
docker-compose up -d
```

This starts a PostgreSQL 15 instance and a Redis instance with the defaults expected by `application.yml`.

### 3. Build the application

```bash
./mvnw clean install -DskipTests
```

### 4. Run the application

```bash
./mvnw spring-boot:run
```

The API is available at `http://localhost:8080`.  
Swagger UI is available at `http://localhost:8080/swagger-ui.html`.

### 5. Package as a fat JAR (optional)

```bash
./mvnw clean package -DskipTests
java -jar target/loan-backend-0.0.1-SNAPSHOT.jar
```

---

## Environment Variables

All variables have safe local-development defaults. Override these in production.

| Variable | Default | Description |
|---|---|---|
| `DB_USERNAME` | `koins` | PostgreSQL username |
| `DB_PASSWORD` | `koins_secret` | PostgreSQL password |
| `REDIS_HOST` | `localhost` | Redis hostname |
| `REDIS_PORT` | `6379` | Redis port |
| `REDIS_PASSWORD` | *(empty)* | Redis password |
| `JWT_SECRET` | *(dev placeholder)* | HS256 signing key — **must be overridden in production** (min 256-bit) |
| `JWT_EXPIRATION_MS` | `86400000` | Token TTL in milliseconds (24 h) |
| `ADMIN_EMAIL` | `admin@koins.com` | Seeded admin account email |
| `ADMIN_PASSWORD` | `Admin@Koins123!` | Seeded admin account password |
| `PAYSTACK_SECRET_KEY` | `sk_test_replace_in_production` | Paystack secret for webhook HMAC verification |
| `LATE_FEE_RATE` | `0.02` | Late fee as a fraction of the instalment (2%) |
| `DEFAULT_THRESHOLD` | `3` | Consecutive overdue instalments before a loan is defaulted |

---

## API Reference

### Authentication — `POST /api/v1/auth`

| Method | Path | Description | Auth |
|---|---|---|---|
| POST | `/auth/register` | Register a new user account | Public |
| POST | `/auth/activate` | Verify OTP and activate account | Public |
| POST | `/auth/login` | Authenticate and receive a JWT | Public |
| POST | `/auth/forgot-password` | Request a password-reset OTP | Public |
| POST | `/auth/reset-password` | Set a new password using the OTP | Public |

### Users — `/api/v1/users`

| Method | Path | Description | Auth |
|---|---|---|---|
| PATCH | `/users/me` | Update display name and/or phone number | Bearer |
| POST | `/users/logout` | Revoke the current JWT | Bearer |

### Wallets — `/api/v1/wallets`

| Method | Path | Description | Auth |
|---|---|---|---|
| GET | `/wallets/me` | Retrieve the authenticated user's wallet | Bearer |
| POST | `/wallets/fund` | Credit the wallet | Bearer + Idempotency-Key |
| GET | `/wallets/{walletId}/transactions` | Paginated transaction history | Bearer |
| GET | `/wallets/transactions/{transactionId}` | Fetch a single transaction | Bearer |

### Loans — `/api/v1/loans`

| Method | Path | Description | Auth |
|---|---|---|---|
| POST | `/loans` | Submit a loan application | Bearer |
| GET | `/loans/me` | List all loans for the authenticated user | Bearer |
| GET | `/loans/{loanId}` | Retrieve a specific loan | Bearer |
| GET | `/loans/{loanId}/schedule` | Fetch the repayment schedule | Bearer |
| POST | `/loans/{loanId}/repay` | Make a repayment | Bearer + Idempotency-Key |
| POST | `/loans/{loanId}/approve` | Approve a pending loan | Admin |
| POST | `/loans/{loanId}/disburse` | Disburse an approved loan | Admin + Idempotency-Key |

### Webhooks — `/api/v1/webhooks`

| Method | Path | Description | Auth |
|---|---|---|---|
| POST | `/webhooks/paystack` | Receive Paystack payment events | HMAC-SHA512 signature |

---

## API Documentation (Swagger)

Interactive API documentation is served by SpringDoc OpenAPI.

| Resource | URL |
|---|---|
| Swagger UI | `http://localhost:8080/swagger-ui.html` |
| OpenAPI JSON | `http://localhost:8080/v3/api-docs` |

All protected endpoints accept a JWT in the Swagger UI. Click **Authorize**, enter `Bearer <your_token>`, and all subsequent requests will be authenticated.

---

## Testing

Tests use an **H2 in-memory database** configured in PostgreSQL compatibility mode — no live database or Redis instance is required to run the test suite.

```bash
# Run all tests
./mvnw test

# Run a specific test class
./mvnw test -Dtest=WalletServiceTest

# Run a specific test method
./mvnw test -Dtest=WalletServiceTest#shouldRejectNegativeBalance
```

Test classes annotated with `@ActiveProfiles("test")` activate the H2 datasource and disable external dependencies. The `create-drop` DDL strategy ensures each test run starts from a clean schema.