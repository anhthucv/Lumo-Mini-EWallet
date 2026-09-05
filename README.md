# Lumo Mini E-Wallet

Lumo is a full-stack mini e-wallet built with Spring Boot and React.

The project supports JWT authentication, internal wallet transfers, payOS top-ups, transaction history, notifications, admin tools, audit logs, and a double-entry ledger.

> Lumo is currently intended to run locally and is not deployed.

---

## Features

### User

- Register with email verification code
- Login with JWT authentication
- View wallet balance
- Transfer money between Lumo wallets
- Add money through payOS
- View transaction history
- Manage beneficiaries
- Receive notifications
- View wallet limits

### Admin

- Admin dashboard
- Search and view users
- Lock / unlock users
- View system transactions
- View audit logs

### Backend

- Double-entry accounting
- Idempotent payment processing
- payOS webhook verification
- Payment status synchronization
- Role-based authorization
- Request correlation IDs
- Swagger / OpenAPI documentation

---

## Tech Stack

### Backend

- Java 21
- Spring Boot 4.1
- Spring Security
- Spring Data JPA / Hibernate
- MySQL
- JWT
- payOS
- JUnit
- Mockito
- Springdoc OpenAPI

### Frontend

- React
- TypeScript
- Vite
- React Router

---

## Project Structure

```text
payment-ledger/
├── payment-ledger-service/   # Spring Boot backend
├── payment-ledger-web/       # React frontend
├── ARCHITECTURE.md           # Architecture and money flows
├── DATA_MODEL.md             # Data model and ledger relationships
└── README.md
```

---

# Running Locally

## 1. Requirements

Install:

- JDK 21
- Node.js and npm
- MySQL
- Git

Optional external services:

- SMTP account for email verification
- payOS account for real Add Money checkout

---

## 2. Clone the Repository

```bash
git clone <YOUR_REPOSITORY_URL>
cd payment-ledger
```

---

## 3. Create a MySQL Database

Create a local database:

```sql
CREATE DATABASE lumo_wallet;
```

The project currently uses Hibernate schema update during development, so the required tables are created or updated when the backend starts.

> For a real production environment, database migrations such as Flyway or Liquibase would normally be preferred.

---

## 4. Configure the Backend

Go to:

```bash
cd payment-ledger-service
```

Set the required environment variables through your terminal, IDE, or local environment configuration.

### Database

Spring Boot standard datasource variables can be used:

```env
SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/lumo_wallet
SPRING_DATASOURCE_USERNAME=root
SPRING_DATASOURCE_PASSWORD=your_mysql_password
```

### JWT

```env
JWT_SECRET=replace_with_a_long_random_secret
JWT_EXPIRATION_MS=3600000
```

`JWT_SECRET` is required for authentication.

---

## 5. Configure Email Verification

Email configuration is required if you want registration OTP emails to be sent.

```env
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your_email@example.com
MAIL_PASSWORD=your_app_password
```

For Gmail, use an App Password instead of your normal account password.

If SMTP is not configured correctly, email verification will not work.

---

## 6. Configure payOS

payOS configuration is only required if you want to test real Add Money checkout.

```env
PAYOS_CLIENT_ID=your_client_id
PAYOS_API_KEY=your_api_key
PAYOS_CHECKSUM_KEY=your_checksum_key

PAYOS_RETURN_URL=http://localhost:5173/payment-result
PAYOS_CANCEL_URL=http://localhost:5173/payment-result
```

These credentials must belong to a payOS Kênh Thu payment channel.

Do not commit real payOS credentials.

---

## 7. Start the Backend

From:

```text
payment-ledger-service/
```

run:

```bash
./mvnw spring-boot:run
```

The backend runs at:

```text
http://localhost:8080
```

Wait until Spring Boot reports that the application has started successfully.

---

## 8. Start the Frontend

Open another terminal:

```bash
cd payment-ledger-web
npm install
npm run dev
```

The frontend runs at:

```text
http://localhost:5173
```

Open that URL in your browser.

---

# Basic Usage

A normal user flow is:

```text
Register
→ Verify email
→ Login
→ Open wallet
→ Add Money or Transfer
→ View Activity
```

---

## Add Money

Open the Wallet page and choose **Add Money**.

With valid payOS credentials:

```text
Lumo
→ payOS checkout
→ User completes payment
→ Lumo verifies provider status
→ Double-entry journal is created
→ Wallet balance is credited
```

The frontend return URL is not considered proof of payment.

Lumo only credits the wallet after the backend verifies the payment with payOS.

---

## Internal Transfer

Transfers occur between Lumo wallets.

```text
Sender wallet
→ Lumo ledger
→ Recipient wallet
```

Current important rules include:

```text
Minimum operation amount: 1,000 VND
Retained wallet balance: 50,000 VND
```

A successful transfer creates a balanced double-entry journal.

---

# Admin Access

New accounts are normal `USER` accounts.

To test the admin interface locally, change one development account to `ADMIN`.

Example:

```sql
UPDATE users
SET role = 'ADMIN'
WHERE email = 'your-test-email@example.com';
```

Then:

1. Log out.
2. Log in again.
3. A new JWT will be issued with the updated role.

Admin pages:

```text
/admin
/admin/users
/admin/transactions
/admin/audit-logs
```

Admin APIs are protected under:

```text
/api/admin/**
```

A normal user receives `403 Forbidden`.

> Only change roles manually in your local development database.

---

# Swagger / OpenAPI

Start the backend and open:

### Swagger UI

```text
http://localhost:8080/swagger-ui.html
```

### OpenAPI JSON

```text
http://localhost:8080/v3/api-docs
```

To test protected endpoints in Swagger:

1. Login to Lumo.
2. Obtain the JWT.
3. Open Swagger UI.
4. Click **Authorize**.
5. Enter the Bearer token.
6. Call the protected APIs.

---

# Main API Areas

## Authentication

```text
POST /auth/register/send-code
POST /auth/register
POST /auth/login
GET  /auth/me
```

## Wallet

```text
GET /wallet/me
GET /wallet/limits
```

The project also contains legacy internal wallet operations.

## Transactions

```text
GET /transactions
GET /transactions/{id}
```

## Beneficiaries

```text
/beneficiaries
```

## Notifications

```text
/notifications
```

## Top-ups

```text
POST /topups
GET  /topups/{id}
POST /topups/{id}/sync
POST /topups/webhook
```

## Admin

```text
GET /api/admin/dashboard
GET /api/admin/users
GET /api/admin/transactions
GET /api/admin/audit-logs
```

See Swagger UI for the complete request and response schemas.

---

# Testing

## Backend

```bash
cd payment-ledger-service
./mvnw test
```

Current test suite:

```text
296 tests
0 failures
0 errors
5 skipped
```

The backend tests cover:

- Authentication
- Authorization
- Wallet rules
- Internal transfers
- Transaction history
- Admin APIs
- Double-entry ledger invariants
- Payment idempotency
- Webhook processing
- Provider reliability

Automated tests do not call real payOS, SMTP, or the remote development database.

---

## Frontend

Build the frontend with:

```bash
cd payment-ledger-web
npm run build
```

---

# Architecture

More detailed technical documentation is available in:

- [ARCHITECTURE.md](ARCHITECTURE.md)
- [DATA_MODEL.md](DATA_MODEL.md)

These documents describe:

- Backend architecture
- Double-entry ledger
- Database relationships
- payOS integration
- Payment provider abstraction
- Top-up flow
- Transfer flow
- Admin and audit architecture

---

# Security Notes

- Passwords are stored as hashes.
- Authentication uses JWT.
- `ROLE_ADMIN` is required for `/api/admin/**`.
- Missing or invalid authentication returns `401`.
- Authenticated users without sufficient permission receive `403`.
- Locked users are rejected by the backend.
- Resource ownership is enforced server-side.
- payOS webhook data is verified before financial finalization.
- Sensitive values are not intentionally written to application logs.

Never commit:

```text
Database passwords
JWT secrets
SMTP passwords
payOS credentials
Bearer tokens
.env files containing real credentials
```

---

# Troubleshooting

## Port 8080 Is Already in Use

On macOS:

```bash
lsof -nP -iTCP:8080 -sTCP:LISTEN
```

Stop the previous backend process and start Spring Boot again.

---

## Frontend API Requests Return 404

If `vite.config.ts` was changed while Vite was running, restart the frontend:

```bash
npm run dev
```

---

## Email Verification Is Not Sending

Check:

```text
MAIL_HOST
MAIL_PORT
MAIL_USERNAME
MAIL_PASSWORD
```

---

## Add Money Does Not Open payOS

Check:

```text
PAYOS_CLIENT_ID
PAYOS_API_KEY
PAYOS_CHECKSUM_KEY
```

Do not print or share the credential values.

---

## Swagger Does Not Open

Confirm the backend is running on:

```text
http://localhost:8080
```

Then try:

```text
http://localhost:8080/swagger-ui.html
```

---

# Scope

Lumo is a **mini e-wallet portfolio project**, not a production banking application.

Currently implemented:

- Email/JWT authentication
- Internal wallet transfers
- payOS incoming top-ups
- Double-entry accounting
- Transaction history
- Notifications
- Beneficiaries
- Limits and risk rules
- Admin tools
- Audit logging

Not included:

- External bank payout
- payOS Kênh Chi
- Refund / reversal flow
- Reconciliation engine
- Production deployment

The existing legacy `/wallet/withdraw` functionality is internal wallet functionality and should not be interpreted as a real bank payout.