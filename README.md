# DevOps Monitoring SaaS

**DevOps Monitoring SaaS** is a multi-tenant monitoring platform that periodically checks HTTP endpoints, records health and latency, opens incidents when checks fail, and can deliver alerts—including **webhooks** on supported plans—while exposing metrics for **Prometheus** and **Grafana**.

## Features

- **Multi-tenancy** — Tenant-scoped data and APIs; JWT carries `tenantId` for isolation
- **Service monitoring** — Register HTTP services with configurable ping intervals; scheduled pings via Redis-backed timing
- **Incident detection** — Failed or slow pings open incidents with causes (e.g. down, degraded, slow)
- **Webhook alerts** — Async JSON POST to a tenant-configured webhook URL when alerts are created (plan-dependent channels)
- **Prometheus / Grafana** — Application metrics via Micrometer; Prometheus scrapes `/actuator/prometheus`; Grafana ships with a provisioned dashboard
- **Role-based access control** — `OWNER`, `ADMIN`, and `MEMBER` roles with method-level `@PreAuthorize` rules

## Technologies

| Area | Stack |
|------|--------|
| Runtime | Java **21** |
| Framework | **Spring Boot** (version in [`build.gradle`](build.gradle)) |
| Data | **PostgreSQL**, Spring Data JPA |
| Cache / coordination | **Redis** (ping scheduling timestamps) |
| Security | Spring Security, **JWT** (HS256) |
| Metrics | Spring Boot Actuator, **Micrometer**, Prometheus registry |
| Observability stack | **Prometheus**, **Grafana**, **Docker** Compose |
| Testing | JUnit 5, Spring Boot Test, **Testcontainers** (PostgreSQL, Redis) |

## Architecture overview

Some SaaS designs use **one PostgreSQL schema per tenant** for hard isolation. This project uses **logical multi-tenancy** instead: a **single shared schema** where entities such as users, services, incidents, and alerts reference a `tenant_id`, and APIs enforce that the authenticated user’s JWT `tenantId` matches the path (see `TenantSecurity`). That trades separate schemas for simpler operations and migrations while keeping tenant data isolated in the application layer.

A **ping scheduler** runs on a fixed cadence, uses **Redis** to track last ping time per service, and only invokes `PingService` when each service’s `pingIntervalSeconds` has elapsed. Pings use **HTTP GET** via `RestTemplate` to external URLs. Unhealthy results drive **incident** creation and **alert** persistence; **WebhookService** may POST a JSON payload to the tenant’s webhook URL.

**Alert delivery** is asynchronous: after an alert is saved, a webhook POST is attempted if a URL is configured and delivery succeeds or is logged on failure.

## Prerequisites

- **Java 21** (Gradle toolchain)
- **Docker** (for Compose: Redis, Prometheus, Grafana, and optionally the app image; **Testcontainers** in CI/tests also need Docker)
- **PostgreSQL** running and reachable (Compose file expects Postgres on the host for the `app` service—see Setup)

## Setup

### 1. Clone the repository

```bash
git clone <repository-url>
cd DevOpsMonitoringSaaS
```

### 2. Configure `application.properties`

Edit [`src/main/resources/application.properties`](src/main/resources/application.properties):

- `spring.datasource.url`, `username`, `password` — your PostgreSQL instance
- `spring.data.redis.host` / `port` — Redis (default `localhost:6379`)
- `jwt.secret` — long random secret in production (must support HS256 key length)

Create a database (e.g. `devopsmonitoring`) if it does not exist.

### 3. Run infrastructure with Docker Compose

From the project root:

```bash
docker compose up -d
```

This starts Redis, Prometheus, Grafana, and the app container (see [`docker-compose.yml`](docker-compose.yml)). **PostgreSQL is not defined in Compose**; the app is configured to connect to PostgreSQL on the host (`host.docker.internal` on Windows/macOS Docker Desktop).

Ensure PostgreSQL is listening on the configured host/port before starting the stack.

### 4. Run the application (alternative: local JVM)

If you prefer not to use the `app` service:

```bash
# Windows
gradlew.bat bootRun

# Linux / macOS / Git Bash
./gradlew bootRun
```

Default HTTP port: **8080**.

## API reference

Base URL: `http://localhost:8080` (unless changed).

Authentication: send `Authorization: Bearer <JWT>` for protected endpoints. Public endpoints are noted below.

Role notes: **Owner** = `OWNER`, **Admin** = `ADMIN`, **Member** = `MEMBER`. “Same tenant” means the JWT `tenantId` must match `{tenantId}` in the path.

### Auth

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| `POST` | `/auth/register` | Register user and company; returns JWT | Public |
| `POST` | `/auth/login` | Login; returns JWT | Public |
| `POST` | `/auth/accept-invite` | Set password from invite token; returns JWT | Public |
| `POST` | `/auth/invite` | Create invitation for email/role | JWT, same tenant, Owner or Admin |

### Tenants

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| `GET` | `/tenants/{tenantId}` | Get tenant profile | JWT, same tenant, Owner / Admin / Member |
| `PUT` | `/tenants/{tenantId}/webhook` | Set or clear webhook URL | JWT, same tenant, **Owner** only |

### Users

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| `GET` | `/tenants/{tenantId}/users` | List users in tenant | JWT, same tenant, Owner / Admin / Member |
| `DELETE` | `/tenants/{tenantId}/users/{userId}` | Remove user | JWT, same tenant, Owner or Admin |
| `PUT` | `/tenants/{tenantId}/users/{userId}/role` | Change user role | JWT, same tenant, Owner or Admin |

### Services

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| `POST` | `/tenants/{tenantId}/services` | Register a monitored service | JWT, same tenant, Owner or Admin |
| `GET` | `/tenants/{tenantId}/services` | List services | JWT, same tenant, Owner / Admin / Member |
| `PUT` | `/tenants/{tenantId}/services/{serviceId}` | Update service | JWT, same tenant, Owner or Admin |
| `DELETE` | `/tenants/{tenantId}/services/{serviceId}` | Delete service | JWT, same tenant, Owner or Admin |

### Incidents

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| `GET` | `/tenants/{tenantId}/incidents` | List incidents | JWT, same tenant, Owner / Admin / Member |
| `GET` | `/tenants/{tenantId}/incidents/{incidentId}` | Get incident | JWT, same tenant, Owner / Admin / Member |
| `GET` | `/tenants/{tenantId}/services/{serviceId}/incidents` | List incidents for a service | JWT, same tenant, Owner / Admin / Member |

### Alerts

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| `GET` | `/tenants/{tenantId}/alerts` | List alerts | JWT, same tenant, Owner / Admin / Member |
| `GET` | `/tenants/{tenantId}/alerts/{alertId}` | Get alert | JWT, same tenant, Owner / Admin / Member |
| `PUT` | `/tenants/{tenantId}/alerts/{alertId}/acknowledge` | Acknowledge alert | JWT, same tenant, Owner or Admin |
| `GET` | `/tenants/{tenantId}/services/{serviceId}/alerts` | Alerts for a service | JWT, same tenant, Owner / Admin / Member |

### Plans

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| `GET` | `/plans` | List subscription plans | Public |
| `POST` | `/tenants/{tenantId}/plan` | Subscribe tenant to a plan | JWT, same tenant, **Owner** |
| `PUT` | `/tenants/{tenantId}/plan` | Change plan | JWT, same tenant, **Owner** |

### Additional (useful for demos)

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| `GET` | `/tenants/{tenantId}/dashboard/summary` | Dashboard summary | JWT, same tenant, Owner / Admin / Member |
| `GET` | `/tenants/{tenantId}/services/{serviceId}/pings` | Ping history (optional filters) | JWT, same tenant, Owner / Admin / Member |
| `GET` | `/actuator/health` | Liveness | Public |
| `GET` | `/actuator/prometheus` | Prometheus scrape endpoint | Public |

## Testing

```bash
# Linux / macOS / Git Bash
./gradlew test

# Windows (Command Prompt / PowerShell)
gradlew.bat test
```

Integration tests use **Testcontainers** (PostgreSQL and Redis); **Docker must be running** locally.

## Monitoring

| Tool | URL | Notes |
|------|-----|--------|
| **Grafana** | [http://localhost:3000](http://localhost:3000) | Default admin user/password are set in [`docker-compose.yml`](docker-compose.yml) (`GF_SECURITY_ADMIN_*`) |
| **Prometheus** | [http://localhost:9090](http://localhost:9090) | Scrapes the app at `/actuator/prometheus` (see [`prometheus.yml`](prometheus.yml)) |

## Project structure

```
src/main/java/com/n/devopsmonitoringsaas/
├── DevOpsMonitoringSaaSApplication.java   # Entry point
├── config/                                 # Security, JPA initializer, RestTemplate, etc.
├── controller/                             # REST API (auth, tenants, services, …)
├── entity/                                 # JPA entities
├── repository/                             # Spring Data repositories
├── service/                                # Business logic (ping, alerts, webhooks, …)
├── scheduler/                              # Ping scheduler
├── security/                               # JWT filter, tenant checks, utilities
├── metrics/                                # Micrometer helpers
└── exception/                              # Application exceptions & handler
```

`src/main/resources/application.properties` — configuration.  
`src/test/` — unit and integration tests.  
`grafana/` — Grafana datasources, dashboard provisioning, JSON dashboard.  
`docker-compose.yml` — local stack.
