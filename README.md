# FinCore — Digital Payment & Wallet Platform

FinCore is a microservices-based digital wallet platform built with Java and Spring Boot. It provides secure user authentication, wallet management, deposits, wallet-to-wallet transfers, transaction history, caching, idempotency, and asynchronous event processing.

The project focuses on backend engineering concepts such as transaction management, concurrency control, distributed communication, reliable event publishing, JWT-based security, and service-level data ownership.

## Architecture

```text
                         ┌─────────────────┐
                         │   Thymeleaf UI  │
                         │   HTML/CSS/JS   │
                         └────────┬────────┘
                                  │
                                  ▼
                         ┌─────────────────┐
                         │   API Gateway   │
                         │    :8080        │
                         └───────┬─────────┘
                                 │
              ┌──────────────────┼──────────────────┐
              │                  │                  │
              ▼                  ▼                  ▼
       ┌─────────────┐    ┌─────────────┐    ┌───────────────┐
       │    User     │    │   Wallet    │    │  Transaction  │
       │   Service   │    │   Service   │    │    Service    │
       │    :8081    │    │    :8082    │    │     :8083     │
       └──────┬──────┘    └──────┬──────┘    └───────┬───────┘
              │                  │                    │
              ▼                  ▼                    ▼
       ┌─────────────┐    ┌─────────────┐    ┌───────────────┐
       │ PostgreSQL  │    │ PostgreSQL  │    │  PostgreSQL   │
       │ fincore_user│    │fincore_wallet│   │fincore_transaction│
       └─────────────┘    └─────────────┘    └───────────────┘

                         ┌─────────────┐
                         │    Redis    │
                         │    Cache    │
                         └─────────────┘

                         ┌─────────────┐
                         │    Kafka    │
                         │    Events   │
                         └─────────────┘
Services
User Service

Responsible for:

User registration
User login
Password hashing using BCrypt
JWT generation
Role-based authorization
User profile endpoint
Publishing UserCreated events
Wallet Service

Responsible for:

Wallet creation
Wallet balance
Deposits
Wallet-to-wallet transfers
Balance validation
Concurrency control
Idempotency
Redis caching
Transaction event creation
Outbox event publishing
Transaction Service

Responsible for:

Consuming transaction events from Kafka
Maintaining transaction history
Querying transactions by reference ID
Querying transactions associated with a wallet
API Gateway

Provides a single entry point for the application.

It:

Routes requests to backend services
Validates JWT authentication
Protects /api/** endpoints
Routes the Thymeleaf UI
Keeps the browser on a single origin
FinCore UI

A lightweight Thymeleaf-based web interface providing:

Registration
Login
Dashboard
Wallet balance
Deposit
Transfer
Transaction history
Logout
Technology Stack
Technology	Purpose
Java 21	Backend development
Spring Boot 4.1.1	Application framework
Spring Security	Authentication and authorization
JWT	Stateless authentication
Spring Data JPA	Persistence
Hibernate	ORM
PostgreSQL 16	Persistent storage
Redis 7	Caching and idempotency
Apache Kafka 4.1.0	Asynchronous events
Spring Cloud Gateway	API Gateway
Thymeleaf	Web UI
Docker	Containerization
Maven	Build management
Authentication Flow
Client
  │
  │ POST /api/auth/login
  ▼
User Service
  │
  ├── Validate credentials
  ├── BCrypt password verification
  └── Generate JWT
  │
  ▼
Client receives JWT
  │
  │ Authorization: Bearer <JWT>
  ▼
API Gateway
  │
  ├── Validate JWT
  └── Route request
  │
  ▼
Target Service

JWT contains the authenticated user's UUID and role.

The application uses stateless authentication, so server-side HTTP sessions are not used for API authentication.

User Registration & Automatic Wallet Creation
Register User
     │
     ▼
User Service
     │
     ├── Save User
     └── Save UserCreated Outbox Event
              │
              ▼
          PostgreSQL
              │
              ▼
       Outbox Publisher
              │
              ▼
            Kafka
              │
              ▼
      Wallet Service Consumer
              │
              ▼
       Create Wallet
       Balance = 0.00

The user and the outbox event are saved within the same local database transaction.

This prevents a successful user creation from losing the corresponding event because of a failure between database persistence and Kafka publishing.

Money Transfer Flow
Client
  │
  │ Transfer Request
  ▼
API Gateway
  │
  ▼
Wallet Service
  │
  ├── Validate request
  ├── Check idempotency key
  ├── Lock both wallets
  ├── Check sender balance
  ├── Debit sender
  ├── Credit receiver
  ├── Save Outbox Event
  └── Commit transaction
          │
          ▼
      Outbox Publisher
          │
          ▼
         Kafka
          │
          ▼
 Transaction Service
          │
          ▼
 Save Transaction History

The Wallet Service is the source of truth for wallet balances.

The Transaction Service maintains transaction history based on transaction events.

Transaction Consistency

Wallet transfers use a database transaction:

BEGIN
  Lock sender wallet
  Lock receiver wallet
  Validate balance
  Debit sender
  Credit receiver
  Save outbox event
COMMIT

If a failure occurs before commit, the database transaction rolls back the balance changes.

The application does not use a distributed database transaction across microservices.

Concurrency Control

Wallet transfers use pessimistic locking.

Both wallet rows are locked before modifying balances.

To reduce deadlock risk, wallets are always locked in deterministic UUID order:

smaller UUID
     ↓
larger UUID

This prevents two concurrent transfers from acquiring the same two locks in opposite orders.

The Wallet entity also contains a JPA @Version field for optimistic locking support.

Idempotency

Transfer requests require an:

Idempotency-Key

Example:

Idempotency-Key: transfer-123

The key is stored durably with the outbox event and also cached in Redis.

If a client retries the same request with the same idempotency key, the existing transaction response can be returned instead of performing another transfer.

A database uniqueness constraint provides durable duplicate protection.

Redis Caching

Redis uses a cache-aside strategy.

GET Wallet
    │
    ▼
Redis
 ┌──┴──┐
Hit   Miss
 │      │
 ▼      ▼
Return PostgreSQL
         │
         ▼
       Redis
         │
         ▼
       Return

PostgreSQL remains the source of truth.

Wallet cache entries are invalidated after balance-changing operations.

Redis is therefore an optimization rather than the authoritative storage for wallet balances.

Kafka & Outbox Pattern

The application uses the Outbox Pattern to reliably publish important events.

Instead of:

Update DB
   ↓
Publish Kafka

the Wallet/User Service performs:

Update business data
       +
Save Outbox Event
       ↓
     COMMIT
       ↓
Background Publisher
       ↓
      Kafka

If Kafka becomes temporarily unavailable, the event remains in the database and can be published later.

The Transaction Service also handles duplicate events using the transaction reference ID and database uniqueness constraints.



API Endpoints
Authentication
POST /api/auth/register
POST /api/auth/login
User
GET /api/users/me
GET /api/admin/dashboard
Wallet
GET  /api/wallets/{walletId}
GET  /api/wallets/user/{userId}
POST /api/wallets/{walletId}/deposit
POST /api/wallets/transfer
Transactions
GET /api/transactions/{referenceId}
GET /api/transactions/wallet/{walletId}

Protected endpoints require:

Authorization: Bearer <JWT>

Transfers additionally require:

Idempotency-Key: <unique-key>
Running with Docker
Prerequisites
Docker Desktop
Java 21
Maven
Environment Variables

Create a .env file in the project root:

POSTGRES_PASSWORD=your_postgres_password
JWT_SECRET=your_base64_jwt_secret
JWT_EXPIRATION=3600000

Never commit .env to Git.

A template is provided in:

.env.example
Start the application
docker compose up -d --build

Check containers:

docker compose ps
Application

Open:

http://localhost:8080

The API Gateway acts as the single browser entry point.

Docker Services
Container	Internal Port	Host Port
API Gateway	8080	8080
User Service	8081	8081
Wallet Service	8082	8082
Transaction Service	8083	8083
FinCore UI	8090	8091
PostgreSQL	5432	15432
Redis	6379	6380
Kafka	9092	9092

Key Backend Concepts Demonstrated:
Microservices architecture
Service-level data ownership
JWT authentication
Role-based authorization
BCrypt password hashing
Database transactions
Pessimistic locking
Optimistic locking
Deadlock prevention
Idempotency
Redis cache-aside pattern
Kafka event-driven communication
Transactional Outbox Pattern
Database constraints
REST API design
Docker containerization
API Gateway routing


Project Structure:
fincore/
│
├── api-gateway/
├── user-service/
├── wallet-service/
├── transaction-service/
├── fincore-ui/
│
├── docker-compose.yml
├── init.sql
├── .env.example
├── .gitignore
└── README.md

Author

Tarun Joshi

Java | Spring Boot | Backend Development | Microservices
