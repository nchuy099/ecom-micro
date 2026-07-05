# Ecom Microservice

E-commerce backend built as a Spring Boot microservice system. The project
covers catalog management, customer orders, authentication, asynchronous order
fulfilment, flash sales and notification delivery.

## Services

| Service | Responsibility |
| --- | --- |
| `service-registry` | Eureka service discovery |
| `api-gateway` | Routing, JWT identity propagation and Redis rate limiting |
| `auth-service` | Keycloak realm integration |
| `user-service` | User profiles, roles, tiers and search |
| `product-service` | Product catalog, cache and stock reservations |
| `order-service` | Orders, idempotency, saga coordination and flash sales |
| `notification-service` | Order notifications, channel fan-out and DLQ replay |
| `common` | Shared API envelope and Kafka event contracts |

## Architecture

Services persist their own data in MySQL through JPA and Flyway migrations.
Order and Product exchange state through Kafka events. The Order Service writes
outbox records transactionally; Debezium routes those records to Kafka for the
reservation and payment saga. Redis is used for the gateway token bucket,
product caching and atomic flash-sale stock checks.

```text
Client -> API Gateway -> Service Registry -> domain services
                           |
             MySQL + Redis + Kafka + Debezium
```

The notification flow fans an order event into email, SMS and push requests.
Provider failures are retried, stored in a DLQ and can be replayed through the
notification admin API.

## Prerequisites

- Java 17
- Docker and Docker Compose

## Build and Test

Each service owns its Gradle configuration. Run the whole suite from the root:

```bash
./gradlew build
```

Run a single service or its tests with its Gradle project path:

```bash
./gradlew :order-service:bootRun
./gradlew :product-service:test
```

## Local Infrastructure

Start MySQL, Redis, Kafka, Debezium and Keycloak:

```bash
docker compose up -d
docker compose ps
```

The Debezium connector definition lives at
`docker/debezium/connectors/order-outbox.json`. The isolated smoke environment
is defined in `docker/compose.smoke.yml` and uses high localhost ports so it
does not collide with the normal local stack.

## Core Workflows

- Product CRUD with cache invalidation and stock reservation.
- Idempotent order creation through `Idempotency-Key`.
- Kafka saga with stock reservation, payment completion/failure and
  compensation.
- Resilience4j retry and circuit breaker around Product Service calls.
- Redis token-bucket rate limiting at the gateway.
- Redis Lua flash-sale purchase gate that caps successful orders at campaign
  stock.
- Notification fan-out with retries, DLQ storage and replay.
