# Lumo Mini E-Wallet

Lumo is a full-stack mini e-wallet built with Spring Boot and React.

Main features include JWT authentication, internal wallet transfers, payOS top-ups, transaction history, notifications, admin tools, audit logs, and a double-entry ledger.

---

## Tech Stack

**Backend**
- Java 21
- Spring Boot 4.1
- Spring Security
- Spring Data JPA / Hibernate
- MySQL
- JWT
- payOS
- JUnit / Mockito
- OpenAPI / Swagger

**Frontend**
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
├── ARCHITECTURE.md
├── DATA_MODEL.md
└── README.md
```

---

# Run Locally

## Requirements

Install:

- JDK 21
- Node.js + npm
- MySQL
- Git

---

## 1. Clone

```bash
git clone <YOUR_REPOSITORY_URL>
cd payment-ledger
```

---

## 2. Create Database

```sql
CREATE DATABASE lumo_wallet;
```

---

## 3. Configure Environment Variables

Backend:

```env
SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/lumo_wallet
SPRING_DATASOURCE_USERNAME=root
SPRING_DATASOURCE_PASSWORD=your_password

JWT_SECRET=your_jwt_secret
JWT_EXPIRATION_MS=3600000

MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your_email
MAIL_PASSWORD=your_app_password

PAYOS_CLIENT_ID=your_payos_client_id
PAYOS_API_KEY=your_payos_api_key
PAYOS_CHECKSUM_KEY=your_payos_checksum_key

PAYOS_RETURN_URL=http://localhost:5173/payment-result
PAYOS_CANCEL_URL=http://localhost:5173/payment-result
```

SMTP variables are required for email verification.

payOS variables are required for real Add Money checkout.

---

## 4. Run Backend

```bash
cd payment-ledger-service
./mvnw spring-boot:run
```

Backend:

```text
http://localhost:8080
```

---

## 5. Run Frontend

Open another terminal:

```bash
cd payment-ledger-web
npm install
npm run dev
```

Frontend:

```text
http://localhost:5173
```

---

## Admin Access

New users have the `USER` role by default.

For local testing, change a test account to `ADMIN`:

```sql
UPDATE users
SET role = 'ADMIN'
WHERE email = 'your-test-email@example.com';
```

Then log out and log in again.

Admin pages:

```text
/admin
/admin/users
/admin/transactions
/admin/audit-logs
```

---

## Swagger

After starting the backend:

```text
http://localhost:8080/swagger-ui.html
```

OpenAPI JSON:

```text
http://localhost:8080/v3/api-docs
```

---

## Testing

Backend:

```bash
cd payment-ledger-service
./mvnw test
```

Frontend:

```bash
cd payment-ledger-web
npm run build
```

---

## Features

- Email verification and JWT authentication
- Wallet balance
- Internal wallet transfers
- payOS Add Money
- Transaction history
- Beneficiaries
- Notifications
- Limits and risk checks
- Double-entry ledger
- Admin dashboard
- Admin user management
- Admin transaction viewer
- Audit logs
- Swagger / OpenAPI
- Request correlation logging

---

## Scope

Lumo is a mini e-wallet portfolio project.

External bank payout, refund/reversal, reconciliation, and production deployment are not included.

The existing legacy `/wallet/withdraw` functionality is internal wallet functionality and should not be interpreted as a real bank payout.