# Lumo Mini E-Wallet

Lumo is a full-stack mini e-wallet built with **Spring Boot and React**.

It supports authentication, wallet transfers, PayOS top-ups, transaction history, admin tools, audit logging, and a double-entry ledger.

## Live Demo

**Frontend:**  
https://lumo-mini-e-wallet.vercel.app

**Backend API:**  
https://lumo-api-zu09.onrender.com

> The backend is hosted on a Render free instance, so the first request after inactivity may take longer.

## Features

- Email verification and JWT authentication
- Wallet balance management
- Internal wallet transfers
- PayOS wallet top-ups
- Webhook-based payment confirmation
- Transaction history
- Beneficiaries and notifications
- Transaction limits and basic risk checks
- Double-entry ledger
- Admin dashboard and audit logs
- Swagger / OpenAPI

## Tech Stack

**Backend:** Java 21, Spring Boot, Spring Security, Spring Data JPA, MySQL, JWT, PayOS

**Frontend:** React, TypeScript, Vite, React Router

**Deployment:** Vercel, Render, Aiven MySQL, Brevo

## Project Structure

```text
payment-ledger/
├── payment-ledger-service/   # Spring Boot backend
├── payment-ledger-web/       # React frontend
├── ARCHITECTURE.md
├── DATA_MODEL.md
└── README.md
```

## Scope

Lumo is a **educational mini e-wallet project**.

The project includes internal wallet transfers, PayOS top-ups, transaction history, double-entry ledger records, basic risk controls, admin tools, and cloud deployment.

The following are outside the current scope:

- Real external bank payouts
- Refund and reversal workflows
- Automated financial reconciliation
- Production-grade banking infrastructure
- Regulatory and compliance processes required by real financial institutions

The existing `/wallet/withdraw` feature represents internal wallet functionality and is not a real bank payout.

Although the project is publicly deployed for demonstration purposes, it is not intended to operate as a real-world financial service.
