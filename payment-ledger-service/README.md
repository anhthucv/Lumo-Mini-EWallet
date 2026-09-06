# Payment Ledger Service

Payment Ledger Service is the backend API for **Lumo Mini E-Wallet**, a digital wallet application supporting authentication, wallet management, money transfers, top-ups, transaction history, and external payment integration.

The application is deployed and accessible online.

## Live Demo

**Frontend**

https://lumo-mini-e-wallet.vercel.app

**Backend API**

https://lumo-api-zu09.onrender.com

## Key Features

- User registration and email verification
- JWT authentication
- Wallet balance management
- Money transfers between users
- Transaction history
- Wallet top-up via PayOS
- PayOS webhook payment confirmation
- Idempotent payment processing
- Transaction limits and basic risk controls
- Role-based admin APIs

## Tech Stack

**Backend**
- Java 21
- Spring Boot
- Spring Security
- Spring Data JPA
- Hibernate
- Maven
- MySQL

**Infrastructure & Services**
- Render
- Aiven MySQL
- PayOS
- Brevo
- Docker

**Frontend**
- React
- TypeScript
- Vite
- Vercel

## Architecture

```text
React Frontend (Vercel)
        |
        v
Spring Boot API (Render)
        |
        +---- MySQL (Aiven)
        |
        +---- PayOS
        |
        +---- Brevo