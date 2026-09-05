# Lumo Mini E-Wallet

Lumo is a full-stack mini e-wallet built with Spring Boot and React.

It supports user authentication, wallet transfers, payOS top-ups, transaction history,
notifications, admin tools, and a double-entry ledger.

> This is a portfolio project intended for learning and demonstration purposes.

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
- Receive deposit and transfer notifications
- View wallet limits

### Admin

- View and search users
- Lock / unlock users
- View system transactions
- View dashboard statistics
- View audit logs

### Backend

- Double-entry ledger
- Idempotent payment processing
- payOS webhook verification
- Top-up status synchronization
- Role-based authorization
- Request correlation IDs
- Swagger / OpenAPI documentation

---

## Tech Stack

### Backend

- Java 21
- Spring Boot 4
- Spring Security
- Spring Data JPA
- MySQL
- JWT
- payOS
- JUnit / Mockito
- OpenAPI / Swagger

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
├── DATA_MODEL.md             # Database model and ledger relationships
└── README.md