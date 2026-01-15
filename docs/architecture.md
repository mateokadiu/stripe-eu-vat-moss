# Architecture

```
                                Operator (CLI / REST)
                                       |
                                       v
+--------------------------------------+----------------------------------+
|                                  Spring Boot                            |
|                                                                         |
|  +----------+   +---------+   +-----------+   +--------+   +--------+   |
|  |  ingest  +-->+ enrich  +-->+  ledger   +-->+  file  +-->+ observe|   |
|  |  CSV/API |   | VIES,   |   | Postgres  |   | SAF-OSS|   | Promet.|   |
|  |  webhook |   | ECB,    |   | events +  |   | XML +  |   | OTel   |   |
|  |          |   | evidence|   | bitemporal|   | XSD    |   | Grafana|   |
|  +----------+   +---------+   +-----------+   +--------+   +--------+   |
|        ^             ^             ^               ^           ^        |
|        |             |             |               |           |        |
|        +-------------+-------------+---------------+-----------+        |
|                           shared (Money, Country, Period)               |
|                                                                         |
+-------------------------------------------------------------------------+
```

## Modules

Each module is a Gradle project. Inter-module dependencies go one
direction; ArchUnit enforces that `moss-it/arch/ModuleBoundariesTest`.

- `moss-shared` — value objects (Money in minor units, Country, Period,
  Iso4217Currency, Ids/UUIDv7).
- `moss-ledger` — append-only `events` table, bitemporal columns,
  `BitemporalRepository` for "as-of" replay. JOOQ DSL, no JPA.
- `moss-enrich` — VIES validator with 24h cache, ECB daily-rate puller +
  quarter-close pin, evidence collector with conflict detection,
  place-of-supply resolver, marketplace deemed-seller router.
- `moss-ingest` — Stripe CSV parser, Standalone Tax API orchestrator,
  webhook handler with signature verification + idempotency, resumable
  cursor.
- `moss-file` — SAF-OSS JAXB types, marshaller with XSD validation gate,
  generator that aggregates supplies by (MS, rate) and corrections by
  (originalPeriod, MS), corrections router.
- `moss-observe` — Micrometer dual registry (Prometheus + OTLP) +
  domain-specific counters and summaries; Grafana dashboard JSON.
- `moss-api` — Spring Boot REST endpoints + OpenAPI yaml.
- `moss-cli` — Picocli operator surface (`ingest-csv`, `close-period`,
  `generate-return`, `audit-replay`).
- `moss-it` — cross-module integration tests + ArchUnit module-boundary
  enforcement.

## Bitemporality

Every aggregate state event has four time columns:

- `valid_time_from` / `valid_time_to` — when the fact was true in the
  real world. Refunds and corrections set `valid_time_from` to the
  refund-issuance instant; the original sale's row keeps the original
  `valid_time_from`.
- `transaction_time_from` / `transaction_time_to` — when the row was
  recorded. Supersedure (e.g. a corrected place-of-supply decision)
  closes the prior row by setting `transaction_time_to` and inserts a
  new row with `transaction_time_from = now`.

The repository supports `loadStreamAsOf(aggregateId, asOf)` which
returns the events that were visible at a given transaction time.
Replays at the same as-of time always produce the same state.

## Event-store

Single `events` table with a unique `(aggregate_id, version)`
constraint enforces optimistic concurrency. Payload and metadata are
JSONB. A `snapshots` table caches per-aggregate state for performance —
events are still the source of truth, snapshots are an optimisation.

## Read model

The SAF-OSS generator queries a denormalised projection built by a
periodic projector that scans the events table. The projector is
idempotent: re-running it overwrites the projection table, which is why
snapshots and rate-pinning matter — they freeze the inputs the
projection depends on.

## Deployment

- Single fat-jar (`./gradlew :moss-api:bootJar`)
- Single Docker image (`./gradlew :moss-api:jib`)
- Postgres 16 via `DATABASE_URL`
- Configuration through environment variables — no credentials in
  repository.

See `docs/deploy.md` for the Oracle Cloud Always Free recipe and
Kubernetes manifest.
