# Payment Ledger Service

Payment Ledger Service is a backend service for managing accounts and basic ledger operations.

## Tech Stack

- Java 21
- Spring Boot
- Maven Wrapper
- REST API

## Requirements

Before running the project, make sure you have:

- JDK installed
- Git installed

You do not need to install Maven globally because this project uses Maven Wrapper.


## Run Application

Run the application with Maven Wrapper:

```bash
./mvnw spring-boot:run
```

If the application starts successfully, it will run at:

```text
http://localhost:8080
```

## Health Check API

### Request

```http
GET /health
```

Full URL:

```text
http://localhost:8080/health
```

### Response

```json
{
  "service": "payment-ledger-service",
  "status": "UP",
  "version": "0.0.1"
}
```

## Test with curl

```bash
curl http://localhost:8080/health
```

Or check the HTTP status:

```bash
curl -i http://localhost:8080/health
```

Expected status:

```text
HTTP/1.1 200
```

## Project Structure

```text
payment-ledger-service
├── src
│   ├── main
│   │   ├── java
│   │   └── resources
│   └── test
├── pom.xml
├── mvnw
├── mvnw.cmd
└── README.md
```
