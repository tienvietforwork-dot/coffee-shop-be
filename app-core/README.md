# Coffee Shop Management System — Backend

A Spring Boot backend for a coffee-shop management system (DATN / thesis project): sales &
shipping workflow, user management with roles, material/inventory management with a
background scheduler, realtime notifications over WebSocket/STOMP, and revenue/product
statistics.

## Tech stack & design decisions

- **Java 17 + Spring Boot 3.3 + Maven** — hand-written `pom.xml` (no Spring Initializr needed).
- **PostgreSQL** as the only supported database, with **Flyway** migrations
  (`src/main/resources/db/migration`) as the single source of truth for the schema. Hibernate
  is configured with `ddl-auto: validate` — it never creates or alters tables itself; Flyway
  owns the schema. This is what makes the database reproducible: point the app at *any* empty
  Postgres instance (local Docker, Neon, Supabase, Render Postgres, ...) and on startup Flyway
  runs `V1__init_schema.sql` then `V2__seed_data.sql` and you get an identical schema + demo
  data, with zero manual SQL steps.
- **Spring Security + JWT** (`io.jsonwebtoken` / jjwt) for stateless auth. Passwords are hashed
  with BCrypt. Roles are `ADMIN`, `STAFF`, `SHIPPER`.
- **Spring WebSocket (STOMP + SockJS)** for realtime order and inventory notifications.
- **`@EnableScheduling`** background jobs for inventory alerts and nightly revenue rollups.
- **springdoc-openapi** for Swagger UI (`/swagger-ui.html`).
- Plain manual DTO <-> entity mapping (no MapStruct) — the domain is small enough that adding
  a mapping-generation dependency would add build complexity without real benefit.
- Lombok is used throughout entities/DTOs to cut boilerplate (`@Getter/@Setter/@Builder`).
- IDs use `BIGSERIAL`/`IDENTITY` (simple auto-increment) rather than UUIDs, appropriate for a
  single-shop system of this scale.
- `product_materials` (the recipe/BOM table) uses a composite key
  (`@IdClass`) of `(product_id, material_id)`.
- Order status flow is linear and one-directional except for cancellation:
  `PENDING → CONFIRMED → PREPARING → READY → COMPLETED`, with `CANCELLED` reachable from any
  non-terminal state. Material stock is only deducted (and only then) when an order transitions
  to `COMPLETED`, based on each product's recipe (`product_materials`), and a `material_transactions`
  row of type `OUT` is written per material consumed — this is the audit trail.

## Project layout

```
src/main/java/com/coffeeshop/
  config/         Spring Security, WebSocket (STOMP), OpenAPI configuration
  security/       JWT util, JWT auth filter, UserDetailsService, UserPrincipal
  entity/         JPA entities (+ entity/enums for Role, OrderStatus, ...)
  repository/     Spring Data JPA repositories
  dto/request/    Request DTOs (validated with jakarta.validation)
  dto/response/   Response DTOs (entity -> DTO mapping lives as static `from(...)` methods)
  service/        Business logic (transactional boundaries live here)
  controller/     REST controllers
  scheduler/      @Scheduled background jobs
  websocket/      NotificationService wrapping SimpMessagingTemplate
  exception/      Custom exceptions + @RestControllerAdvice global handler
src/main/resources/
  application.yml         Base config, reads everything from env vars with sane defaults
  application-local.yml   Example profile for local dev against a localhost Postgres
  db/migration/           Flyway SQL migrations (V1 = schema, V2 = seed data)
src/test/java/...         Context-load smoke test + one service unit test
```

## Running locally

### Option A — Docker Compose (recommended, one command)

Requires Docker Desktop.

```bash
docker compose up --build
```

This starts a Postgres 16 container and the backend container together. Flyway will migrate
the (initially empty) `coffeeshop` database automatically on the backend's first boot. The API
is then available at `http://localhost:8080`, and Swagger UI at
`http://localhost:8080/swagger-ui.html`.

Default seeded logins (see `V2__seed_data.sql`, all use the same demo password):

| username   | password   | role    |
|------------|------------|---------|
| `admin`    | `admin123` | ADMIN   |
| `staff1`   | `admin123` | STAFF   |
| `shipper1` | `admin123` | SHIPPER |

**Change these before any real deployment.**

### Option B — Run Postgres via Docker, backend via Maven

```bash
docker compose up -d db
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

`application-local.yml` points at `localhost:5432/coffeeshop` with the `postgres/postgres`
credentials used by `docker-compose.yml`.

### Option C — fully manual

1. Create an empty Postgres database.
2. Export the env vars listed below (or create a `.env` from `.env.example` and load it into
   your shell / IDE run configuration).
3. `mvn spring-boot:run`

In every option, **you never write any DDL by hand** — Flyway creates the schema on first
startup against whatever empty database it's pointed at.

## Required environment variables

| Variable                     | Description                                              | Default (application.yml)                     |
|-------------------------------|-----------------------------------------------------------|------------------------------------------------|
| `SPRING_DATASOURCE_URL`       | JDBC URL of the Postgres database                         | `jdbc:postgresql://localhost:5432/coffeeshop`   |
| `SPRING_DATASOURCE_USERNAME`  | DB username                                                | `postgres`                                      |
| `SPRING_DATASOURCE_PASSWORD`  | DB password                                                | `postgres`                                      |
| `JWT_SECRET`                  | HMAC secret used to sign JWTs (>= 32 chars recommended)    | insecure placeholder — **override in prod**     |
| `JWT_EXPIRATION_MS`           | JWT lifetime in milliseconds                               | `86400000` (24h)                                |
| `CORS_ALLOWED_ORIGIN`         | Origin allowed to call the API / connect the websocket     | `http://localhost:5173`                         |
| `PORT`                        | Port the server listens on (Render sets this automatically)| `8080`                                          |
| `SERVER_PORT`                 | Fallback port if `PORT` is not set                         | `8080`                                          |

See `.env.example` for a copy-pasteable list.

## API endpoints

All endpoints are prefixed `/api`. JSON in/out. Send `Authorization: Bearer <token>` for
everything except `/api/auth/login` and public `GET` product/category listings.

### Auth
- `POST /api/auth/login` — `{ username, password }` → `{ token, username, role, userId }`

### Users (ADMIN only)
- `GET /api/users`, `GET /api/users/{id}`
- `POST /api/users`, `PUT /api/users/{id}`, `DELETE /api/users/{id}`

### Categories
- `GET /api/categories` (public), `GET /api/categories/{id}` (public)
- `POST/PUT/DELETE /api/categories(/...)` (ADMIN)

### Products
- `GET /api/products` (public), `GET /api/products/{id}` (public)
- `POST/PUT/DELETE /api/products(/...)` (ADMIN)
- `PUT /api/products/{id}/recipe` (ADMIN) — replace the product's material recipe:
  body `[{ materialId, quantityRequired }, ...]`

### Materials / inventory (ADMIN manages, STAFF can record transactions)
- `GET /api/materials`, `GET /api/materials/{id}`
- `GET /api/materials/low-stock` — materials currently below `min_threshold`
- `POST/PUT/DELETE /api/materials(/...)` (ADMIN)
- `POST /api/materials/transactions` — `{ materialId, type: IN|OUT|ADJUST, quantity, reason }`
  (ADMIN, STAFF)

### Orders (ADMIN, STAFF)
- `GET /api/orders`, `GET /api/orders/{id}`
- `POST /api/orders` — `{ customerName, customerPhone, orderType, items: [{ productId, quantity }] }`
  — validates stock availability against product recipes before allowing the order
- `PATCH /api/orders/{id}/status` — `{ status }` — on transition to `COMPLETED`, stock is
  deducted transactionally and a `material_transactions` row is written per material consumed

### Shipments (ADMIN, STAFF, SHIPPER — status updates restricted to ADMIN/SHIPPER)
- `GET /api/shipments`, `GET /api/shipments/{id}`
- `POST /api/shipments` — `{ orderId, address }`
- `PATCH /api/shipments/{id}/assign` — `{ shipperId, address? }`
- `PATCH /api/shipments/{id}/status` — `{ status, note? }`

### Payments
- `GET /api/payments?orderId=...`
- `POST /api/payments` — `{ orderId, method, amount }`

### Reports (ADMIN, STAFF)
- `GET /api/reports/revenue?from=YYYY-MM-DD&to=YYYY-MM-DD`
- `GET /api/reports/top-products?from=YYYY-MM-DD&to=YYYY-MM-DD&limit=5`

### Ops
- `GET /actuator/health` — health check (used by Render)
- `GET /swagger-ui.html`, `GET /v3/api-docs` — OpenAPI docs

## WebSocket / realtime

STOMP over SockJS endpoint: `/ws` (CORS-restricted to `app.cors.allowed-origin`).

Topics clients subscribe to:
- `/topic/orders` — broadcast whenever an order is created or its status changes
- `/topic/inventory-alerts` — broadcast when a material's stock crosses below its
  `min_threshold` (both from a direct stock-changing action and from the periodic scheduler
  below)

Example client connect (JS, using `sockjs-client` + `@stomp/stompjs`):
```js
const socket = new SockJS('http://localhost:8080/ws');
const client = new Client({ webSocketFactory: () => socket });
client.onConnect = () => {
  client.subscribe('/topic/orders', msg => console.log(JSON.parse(msg.body)));
  client.subscribe('/topic/inventory-alerts', msg => console.log(JSON.parse(msg.body)));
};
client.activate();
```

## Background scheduled jobs

Enabled via `@EnableScheduling` on the main application class.

- **`InventoryAlertScheduler`** (`fixedRate = 5 minutes`): scans all materials for
  `quantity_in_stock < min_threshold` and pushes an alert to `/topic/inventory-alerts`.
  To avoid spamming, it keeps an in-memory set of currently-low material IDs and only
  broadcasts for materials that are *newly* below threshold since the previous scan (a
  simple in-process flag — fine for a single-instance deployment; a multi-instance
  deployment would need to move this state into the database or a shared cache).
- **`RevenueAggregationScheduler`** (`cron = "0 5 0 * * *"`, i.e. 00:05 every night):
  aggregates the previous day's `COMPLETED` orders into the `daily_revenue` table
  (upsert), so historical revenue reporting doesn't need to re-scan the `orders` table.
  `/api/reports/revenue` itself always computes directly from `orders` (so "today" is
  always accurate even before the nightly job runs) — `daily_revenue` exists as a
  fast-path/audit table for historical dates and could be swapped in by the report
  endpoint later if `orders` grows large.

## Tests

```bash
mvn test
```

- `CoffeeShopApplicationTests` — full Spring context load, including running the real Flyway
  migrations against an in-memory H2 database in PostgreSQL-compatibility mode
  (`src/test/resources/application-test.yml`). This is the main proof that the schema and
  wiring are self-consistent.
- `MaterialServiceTest` — a focused Mockito unit test of stock IN/OUT/threshold-alert logic.

This is intentionally not exhaustive coverage (per the project scope) — it covers a context
smoke test plus one meaningful service test.

## CI/CD

- `.github/workflows/ci.yml` — runs `mvn test` then `mvn package` on every push/PR to `main`.
- `.github/workflows/deploy.yml` — after CI succeeds on `main`, `curl`s a Render deploy hook
  URL stored in the GitHub secret `RENDER_DEPLOY_HOOK_URL`. Render can also auto-deploy from
  GitHub pushes directly, but this workflow gives an explicit, visible CI/CD pipeline stage
  (useful for a DATN report) and gates the deploy behind CI passing.

  **You must create this secret yourself** after setting up the Render service (see below):
  GitHub repo → Settings → Secrets and variables → Actions → New repository secret →
  name `RENDER_DEPLOY_HOOK_URL`, value = the deploy hook URL from your Render service's
  Settings page.

## Deploying to Render

1. **Create a Postgres database** first (Render's own Postgres, or an external one like
   [Neon](https://neon.tech) or [Supabase](https://supabase.com) — any of these work since the
   app only needs a standard Postgres connection string). Copy its connection string.
2. On [Render](https://render.com), click **New → Web Service**, connect this GitHub repo.
3. **Runtime**: choose **Docker** (Render will build using the `Dockerfile` in the repo root).
4. **Health check path**: set to `/actuator/health`.
5. **Environment variables** — add all of these in the Render service's Environment tab:
   - `SPRING_DATASOURCE_URL` = your Postgres JDBC URL, e.g.
     `jdbc:postgresql://<host>:5432/<db>?sslmode=require` (Neon/Supabase require SSL)
   - `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`
   - `JWT_SECRET` — generate a long random value, e.g. `openssl rand -base64 48`
   - `JWT_EXPIRATION_MS` — e.g. `86400000`
   - `CORS_ALLOWED_ORIGIN` — your deployed frontend's URL
   - Do **not** set `PORT` yourself — Render injects it automatically and
     `application.yml` reads `${PORT:8080}`.
6. Deploy. On first boot, Flyway runs against your (empty) Postgres database and creates the
   full schema + seed data automatically — no manual SQL step required.
7. Once the service is live, go to its **Settings → Deploy Hook**, copy the URL, and add it as
   the `RENDER_DEPLOY_HOOK_URL` GitHub Actions secret described above so `deploy.yml` can
   trigger deploys from CI.

## Remaining manual steps / TODOs for the user

- Create a Render account + web service, and a Postgres instance (Render/Neon/Supabase) —
  cannot be automated from here.
- Add the `RENDER_DEPLOY_HOOK_URL` GitHub secret once the Render service exists.
- Replace `JWT_SECRET` and the seeded demo passwords before using this anywhere near
  production.
- If you need multi-instance horizontal scaling, move the `InventoryAlertScheduler`'s
  "already alerted" tracking out of in-memory state (e.g. a DB column or a shared cache),
  and consider Spring's `ShedLock` (or similar) so scheduled jobs don't run redundantly on
  every instance.
- No frontend is included in this repository — `CORS_ALLOWED_ORIGIN` and the WebSocket
  `SockJS`/STOMP topics are the integration points a frontend should use.
