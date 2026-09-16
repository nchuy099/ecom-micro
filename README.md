# Ecom Microservice

E-commerce backend built as a Spring Boot microservice system. The project
covers catalog management, customer orders, authentication, synchronous stock
reservation, flash sales and notification delivery.

## Services

| Service | Responsibility |
| --- | --- |
| `service-registry` | Eureka service discovery |
| `api-gateway` | Routing, JWT identity propagation and Redis rate limiting |
| `auth-service` | Keycloak integration plus user profiles, roles, tiers and search |
| `product-service` | Product catalog, cache and stock reservations |
| `order-service` | Orders, idempotency, synchronous stock reservation and flash sales |
| `notification-service` | Notification delivery, channel fan-out and DLQ replay |

## Architecture

Services persist their own data through JPA and Flyway migrations. Auth and user
tables share the `auth_service` schema; the other services keep separate schemas.
The local Docker stack runs one MySQL container, one Redis
instance, Kafka and Keycloak. Each service owns its local
`application.yml`; environment variables provide deployment-specific values.
Eureka provides service discovery for the gateway and domain services.

```text
                    +------------------------+
                    |   Client (Web / Mobile) |
                    +------------+-----------+
                                 |
                            HTTPS + JWT
                                 v
                    +------------------------+
                    |   API Gateway :8282    |
                    | Keycloak JWT | Eureka   |
                    | Redis rate limit | LB   |
                    +--+----------+--------+-+
                       |          |          |
        +--------------+----------+----------+--------------+
        |              |          |                         |
        v              v          v                         v
 +-------------+ +-------------+ +-------------+ +----------------+
 | Order       | | Product     | | Auth + User | | Notification   |
 | orders      | | catalog     | | identity +  | | fan-out        |
 | order/sale  | | cache/stock | | profiles    | | retry + DLQ    |
 +------+------+ +------+------+ +-------------+ +----------------+
        |              |          |
        +--------------+----------+
                       |
                       v
          +------------+------------+------------+
          |                         |            |
          v                         v            v
   +-------------+           +-------------+ +-----------+
   | Kafka       |           | Redis       | | MySQL     |
   | notification |           | rate/cache  | | per-svc DB|
   | notif + DLQ |           | idempotency | | source    |
   +-------------+           +-------------+ +-----------+

```

Each service keeps its configuration in `src/main/resources/application.yml`.
Values still use environment placeholders, so deployment settings override the
local defaults after a service restart.

Notification workers retry provider failures, store them in a DLQ and expose
replay through the notification admin API.

## Prerequisites

- Java 17
- Docker and Docker Compose

## Build and Test

Each service is an independent Gradle project with its own wrapper and build
configuration. Run one service from its directory:

```bash
cd order-service
./gradlew build
```

Build every service from the repository root:

```bash
for service in service-registry api-gateway auth-service product-service order-service notification-service; do
  (cd "$service" && ./gradlew build)
done
```

Run a service or its tests from that service directory:

```bash
./gradlew bootRun
./gradlew test
```

## Docker Stack

Start the complete stack, including Eureka, Gateway and every application
service:

```bash
docker compose up --build -d
docker compose ps
```

Application ports are published to the host: Eureka `8761`, Gateway `8282`,
Auth/User `8080`, Product `8082`, Order `8083` and Notification `8084`.
Services use Docker DNS internally; no local
`./gradlew bootRun` process is required.

Infrastructure is also available from the host: MySQL `13306`, Redis `16379`,
Kafka `9092`, Elasticsearch `9200`, and Keycloak `8090`. MySQL and Redis use non-default host ports
to avoid colliding with local installations; containers still use `mysql:3306`
and `redis:6379` internally.

Check the stack health:

```bash
docker compose ps
curl http://localhost:8282/actuator/health
```

Restart an affected service after changing its environment or local config. To
stop the stack, run `docker compose down`; add `-v` only when local MySQL data
should also be removed.

The isolated smoke environment is defined in `docker/compose.smoke.yml` and uses
high localhost ports so it does not collide with the normal local stack.

## Core Workflows

- Product CRUD with cache invalidation and stock reservation.
- Synchronous order placement with pessimistic product-row locking and reservation compensation.
- Elasticsearch-backed product search with an admin-only manual reindex endpoint.
- Cursor-based product and order pagination, while preserving legacy page parameters.
- Composite database indexes and batched order-item loading to avoid N+1 queries.
- Idempotent order creation through `Idempotency-Key`.
- Resilience4j retry and circuit breaker around Product Service calls.
- Redis token-bucket rate limiting at the gateway.
- Redis Lua flash-sale purchase gate that caps successful orders at campaign
  stock.
- Redis-backed distributed campaign locks coordinate flash-sale requests across
  order-service instances.
- Confirmed orders publish notification requests to Kafka; notification workers
  fan out across channels with retries, DLQ storage and replay.
