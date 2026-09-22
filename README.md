# Ecom Microservice

E-commerce backend built as a Spring Boot microservice system with dedicated
Auth, Product, Order and Notification services. The project covers catalog
management, customer orders, authentication, synchronous stock reservation,
flash sales and high-volume notification delivery.

## Services

| Service | Responsibility |
| --- | --- |
| `service-registry` | Eureka service discovery |
| `api-gateway` | Routing, JWT identity propagation and Redis rate limiting |
| `auth-service` | Keycloak integration plus user profiles, roles, tiers and search |
| `product-service` | Product catalog, cache and stock reservations |
| `order-service` | Orders, idempotency, synchronous stock reservation and flash sales |
| `notification-service` | Business-event to task conversion, notification workers, rate limiting and DLQ replay |

## Architecture

Services persist their own data through JPA and Flyway migrations. Auth and user
tables share the `auth_service` schema; the other services keep separate schemas.
The local Docker stack runs one MySQL container, one Redis
instance, Kafka and Keycloak. Each service owns its local
`application.yml`; environment variables provide deployment-specific values.
Eureka provides service discovery for the gateway and domain services, while
Keycloak centralizes identity management and secure service access.

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

Notification processing has two separate paths. Confirmed orders publish a
small business event to `order.events`; notification-service converts it into
email, push and SMS work items on `notification.tasks`. Flash-sale campaigns
generate bulk push work items directly on `notification.tasks`, using the
12-partition priority lanes and a Push provider rate limiter. Both paths retry
temporary provider failures through `notification.retry`, persist failures in
the notification DLQ and expose replay through the notification admin API.

## Core Workflows

- Dedicated Auth, Product, Order and Notification microservices with Eureka,
  Keycloak JWT authentication and role-based access control.
- Redis caching and Elasticsearch product search with cache invalidation and
  manual reindexing.
- Database optimization through N+1 query elimination, composite indexes,
  batched loading and keyset pagination.
- Redis Lua flash-sale purchase gate and distributed locks for concurrent
  purchase control and inventory consistency.
- Kafka-based asynchronous notifications: `order.events` become prioritized
  `notification.tasks` with retry, DLQ and provider rate limiting.
- Admin flash-sale campaigns publish bulk push tasks 15 minutes before launch,
  processed independently from core order workloads.

## Build, Test & Run

Prerequisites: Java 17, Docker and Docker Compose.

```bash
# Build and test one service
cd order-service && ./gradlew build

# Build all services from the repository root
for service in service-registry api-gateway auth-service product-service order-service notification-service; do
  (cd "$service" && ./gradlew build)
done

# Start the full local stack
docker compose up --build -d
docker compose ps
```

Run individual tests with `./gradlew test`; stop the stack with
`docker compose down`. The isolated smoke stack is available at
`docker/compose.smoke.yml`.
