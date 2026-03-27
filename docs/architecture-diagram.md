# Architecture diagram

This document summarizes how the DevOps Monitoring SaaS components interact. The runtime uses **one PostgreSQL database** with **tenant-scoped rows** (`tenant_id` on related entities): requests are authorized so users only access their tenant’s data (JWT `tenantId` + `TenantSecurity`). That is **logical multi-tenancy** in a shared schema—not separate PostgreSQL schemas per tenant.

## Mermaid (component view)

```mermaid
flowchart LR
  subgraph Clients
    U[API Clients / Browsers]
  end

  subgraph SpringBoot[Spring Boot application]
    API[REST Controllers]
    SEC[JWT + RBAC]
    SCH[Ping Scheduler]
    PS[PingService]
    IS[IncidentService]
    AS[AlertService]
    WH[WebhookService]
    MM[Micrometer metrics]
  end

  PG[(PostgreSQL\nshared schema\ntenant_id isolation)]
  RD[(Redis\nlast ping keys)]

  subgraph Observability
    PR[Prometheus]
    GF[Grafana]
  end

  EXT[External HTTP services\nmonitored URLs]

  WH_EP[Webhook endpoints\ncustomer URLs]

  U --> API
  API --> SEC
  API --> PG
  SCH --> RD
  SCH --> PS
  PS --> EXT
  PS --> PG
  PS --> IS
  PS --> AS
  AS --> WH
  WH --> WH_EP
  PS --> MM
  MM --> PR
  PR --> GF
```

## Data flow: monitoring and alerts

```mermaid
sequenceDiagram
  participant S as Ping Scheduler
  participant R as Redis
  participant P as PingService
  participant X as External service
  participant I as Incident detection
  participant A as Alert creation
  participant W as Webhook delivery

  S->>R: Read last ping time per service
  S->>P: ping(service) when interval elapsed
  P->>X: HTTP GET
  X-->>P: Response / error
  alt Unhealthy
    P->>I: Open or attach incident
    P->>A: Create alert
    A->>W: POST JSON to tenant webhook URL
  else Healthy
    P->>I: Close open incident if any
  end
```

## Metrics flow: App → Prometheus → Grafana

```mermaid
flowchart LR
  APP[Spring Boot + Micrometer]
  ACT["/actuator/prometheus"]
  PR[Prometheus scrape]
  GF[Grafana dashboards]

  APP --> ACT
  ACT --> PR
  PR --> GF
```

Micrometer records application metrics; Prometheus pulls from the Actuator endpoint on an interval; Grafana uses Prometheus as a datasource to visualize business and operational metrics.

## ASCII overview

```
                    +------------------+
                    |   API clients    |
                    +--------+---------+
                             |
                             v
+----------------+    +------+------+     +-------------+
|   PostgreSQL   |<---| Spring Boot |---->|    Redis    |
| (tenant rows)  |    | JWT / RBAC  |     | ping timing |
+----------------+    +------+------+     +-------------+
                             |
         +-------------------+-------------------+
         |                   |                   |
         v                   v                   v
 +---------------+   +---------------+   +---------------+
 | Ping scheduler|   | External HTTP |   | Webhook URLs  |
 |  (scheduled)  |   |  (monitored)  |   |  (customer)   |
 +---------------+   +---------------+   +---------------+
                             |
                    Micrometer /actuator/prometheus
                             |
                             v
                    +--------+---------+
                    |    Prometheus    |
                    +--------+---------+
                             |
                             v
                    +--------+---------+
                    |     Grafana      |
                    +------------------+
```

## Multi-tenancy (brief)

- Each **tenant** has its own users, services, incidents, and alerts in the same database; foreign keys and queries are scoped by `tenant_id`.
- After login, the **JWT** includes `tenantId` and `role`; controllers use `@PreAuthorize` and `TenantSecurity.sameTenant(...)` so paths like `/tenants/{tenantId}/...` cannot be used to access another tenant’s data.
