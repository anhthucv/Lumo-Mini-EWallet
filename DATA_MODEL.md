# Lumo Data Model

## ERD

```mermaid
erDiagram
    USER ||--o| ACCOUNT : owns
    USER ||--o{ NOTIFICATION : receives
    USER ||--o{ BENEFICIARY : owns
    USER ||--o{ AUDIT_LOG : acts
    ACCOUNT ||--o{ TRANSACTION : records
    ACCOUNT ||--o{ TOP_UP_PAYMENT : funds
    ACCOUNT ||--o{ LEDGER_ACCOUNT : has
    ACCOUNT ||--o{ RISK_EVENT : evaluates
    TRANSACTION }o--o| ACCOUNT : related_account
    TRANSACTION }o--o| JOURNAL : posts
    TOP_UP_PAYMENT }o--o| TRANSACTION : finalizes_as
    TOP_UP_PAYMENT }o--o| JOURNAL : finalizes_with
    JOURNAL ||--|{ LEDGER_ENTRY : contains
    LEDGER_ACCOUNT ||--o{ LEDGER_ENTRY : receives

    USER { bigint id PK; string email UK; string password_hash; enum role; enum status }
    ACCOUNT { bigint id PK; string account_number UK; decimal balance; enum status }
    TRANSACTION { bigint id PK; bigint account_id FK; bigint related_account_id FK; bigint journal_id FK; enum type; enum status; decimal amount; decimal balance_after_transaction }
    JOURNAL { bigint id PK; string reference UK }
    LEDGER_ACCOUNT { bigint id PK; string code UK; enum type; enum account_class; bigint account_id FK }
    LEDGER_ENTRY { bigint id PK; bigint journal_id FK; bigint ledger_account_id FK; enum entry_type; decimal amount }
    TOP_UP_PAYMENT { bigint id PK; bigint account_id FK; bigint transaction_id FK; bigint journal_id FK; bigint merchant_order_code UK; string provider_reference; enum status; decimal amount; string idempotency_key }
```

## Financial Records

`AccountEntity` is the wallet aggregate containing the current balance, account number, owner name, and operational status. It is not the complete financial history; journals and transactions provide the historical record.

`TransactionEntity` records a user-facing operation, including type, status, amount, balance after the operation, optional related account, and optional journal. A transfer creates sender and recipient rows linked to one journal.

`JournalEntity` groups accounting entries for one operation. Every `LedgerEntryEntity` has an amount, `DEBIT` or `CREDIT` direction, and ledger account. Successful journals satisfy:

```text
sum(DEBIT amounts) = sum(CREDIT amounts)
```

| Flow | Debit | Credit |
| --- | --- | --- |
| Add Money | `PROVIDER_CLEARING` | user `WALLET` |
| Internal transfer | sender `WALLET` | recipient `WALLET` |
| Legacy wallet withdraw | user `WALLET` | `SYSTEM_CLEARING` |

Wallet accounts are liability-class ledger accounts in the current model. Clearing accounts are asset-class accounts.

## Payment Records

`TopUpPaymentEntity` belongs to an account and starts in `PENDING`. It stores the Lumo merchant order code, provider reference, checkout URL, provider, amount, status, idempotency key, and links to the finalized transaction and journal. Its account/idempotency-key uniqueness constraint supports replay protection.

Provider status does not directly mutate the wallet; it passes through the finalization service and must match the persisted order data.

## User-Supporting Records

- `UserEntity`: identity, password hash, role, lock status, and lock metadata.
- `BeneficiaryEntity`: user-owned saved recipient information.
- `NotificationEntity`: user-scoped financial notification, read state, amount, and transaction reference.
- `RiskEventEntity`: operation, amount, risk decision, reason, account, and timestamp.
- `AuditLogEntity`: durable administrative actor, target, action, reason, and metadata history.

## Ownership and Status Rules

Current-user APIs derive ownership from the authenticated JWT principal. Service/repository lookups scope wallet, transaction, top-up, beneficiary, and notification access to that user. Admin APIs are separately protected by the `ADMIN` role.

Top-ups use `PENDING`, `SUCCESS`, and `CANCELLED`. Only a pending top-up can be finalized or cancelled. Once finalized, duplicate or stale provider events are ignored at the finalization boundary.

Transaction statuses follow the existing application transition rules. Idempotency records and top-up uniqueness protect the operations where idempotency is implemented; they do not imply a reconciliation system.
