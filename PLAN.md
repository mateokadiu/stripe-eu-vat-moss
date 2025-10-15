# `stripe-eu-vat-moss` — Implementation Plan

> An open-source **EU VAT One-Stop-Shop automation engine** in **Java 21 + Spring Boot 3.4**. Sits next to a Stripe Tax integration, fills the gaps Stripe stops at: file-ready SAF-OSS XML returns, evidence-of-location storage, VIES B2B validation, refund/correction handling, ECB currency conversion, marketplace deemed-seller routing. Bitemporal audit trail. Free, MIT, self-hostable.

**Status:** Locked. Phase 0 starts.

## Locked decisions

| # | Setting | Locked value |
|---|---|---|
| 1 | Repo + folder name | `stripe-eu-vat-moss` at `~/Desktop/development/personal/stripe-eu-vat-moss/` and `github.com/mateokadiu/stripe-eu-vat-moss` |
| 2 | License | MIT, copyright holder "Mateo Kadiu", year 2026 |
| 3 | Visibility | Public OSS from day 1 |
| 4 | Language | **Java 21 LTS** — use the modern subset (sealed interfaces for events, records for value objects, pattern matching, virtual threads). No Kotlin. |
| 5 | Build tool | **Gradle 8 with Kotlin DSL**. Multi-module. `nemerosa/versioning` plugin for version inference. |
| 6 | Framework | Spring Boot 3.4 + Spring Modulith |
| 7 | Database | Postgres 16 + Liquibase migrations |
| 8 | Persistence | **JOOQ everywhere — no JPA**. JOOQ codegen at build time against the migrated dev DB. |
| 9 | Event store | Custom Postgres `events` table (eugene-khyst pattern), Postgres LISTEN/NOTIFY for cross-process notification |
| 10 | Money | `org.javamoney:moneta` (JSR 354), minor units in DB |
| 11 | IDs | v7 UUID via `com.github.f4b6a3:uuid-creator` |
| 12 | Time | `java.time` only — Instant, LocalDate, ZonedDateTime |
| 13 | HTTP | Java 21 `HttpClient` with virtual threads |
| 14 | Stripe SDK | `stripe-java` (latest) |
| 15 | XML | JAXB via `unbroken-dome/gradle-xjc-plugin` against EU SAF-OSS XSD |
| 16 | Logging | SLF4J + Logback + `net.logstash.logback:logstash-logback-encoder` (JSON structured logs) |
| 17 | Tests | JUnit 5 + AssertJ + Testcontainers (Postgres) + WireMock + jqwik + ArchUnit + **Pitest mutation testing** on compliance surfaces |
| 18 | Static analysis | Spotless (Google Java Format) + Error Prone + NullAway + CodeQL + OWASP Dependency Check |
| 19 | Observability | Micrometer with dual registries (Prometheus + OTel); OpenTelemetry Java agent; Grafana LGTM stack committed as docker-compose |
| 20 | Container | Jib + distroless base (`gcr.io/distroless/java21-debian12`) |
| 21 | IaC | Pulumi in Java — Oracle Cloud Always Free ARM VM + Postgres + Caddy |
| 22 | CLI | Picocli — mirrors REST surface for one-off ops |
| 23 | Single MS-of-identification at v1 | Yes — operator picks one via env var `MOSS_IDENT_MEMBER_STATE` |
| 24 | Deemed-seller default | Off — opt-in per-transaction Stripe metadata override |
| 25 | Evidence-piece minimum | 2 enforced unless `MOSS_SMALL_ENTERPRISE_MODE=true` |
| 26 | VIES cache TTL | 24 hours |
| 27 | Frontend | None in v1 — Grafana for metrics + REST/JSON for everything else |
| 28 | IP geolocation | MaxMind GeoLite2 (free, attribution) bundled into Docker image |
| 29 | Commit timeline | Spread **2026-06-01 → 2026-07-15** (6 weeks, ~50 commits, organic evening cadence) |
| 30 | Author email | `mateokadiu17@gmail.com` everywhere — no exceptions |
| 31 | Attribution | No assistant trace anywhere — commits, comments, PRs, docs, README, code |
| 32 | OpenAPI spec | Generated from controllers via `springdoc-openapi`, committed at `docs/openapi.yaml` |
| 33 | Conventional commits | Lowercase first letter, short subject under 60 chars, body only when necessary, no markdown bullets in body |

---

## 1. Why this exists

Every EU SaaS shop crossing **€10,000 of cross-border B2C sales** must file a **quarterly OSS VAT return** in the Member State of identification. Stripe Tax calculates and exports per-line VAT correctly — but it **does not file the return**. The "last mile" is left to merchants who:

- Log into the national portal (Intervat in Belgium, BZSt in Germany, AT/IE/etc.) and hand-key a quarterly summary
- Or pay €30–100/mo to a commercial SaaS (Quaderno, Octobat, Taxually, Hellotax, Sovos)
- Or spreadsheet it from a Stripe CSV — error-prone, no audit trail, no correction history

**There is no open-source player in this space.** A GitHub search for `vat-moss` / `vat-oss` returns small abandoned scripts. `stripe-eu-vat-moss` fills that gap.

Adjacent to my existing OSS projects (`temporal-stripe` for Stripe Connect workflows, `tax-ledger` for line-item splitting), this becomes the **third leg of a coherent Stripe + tax open-source story**: workflow → splitting → filing.

---

## 2. Goals & non-goals

### Goals (v1.0)

- **Ingest** Stripe Tax data — both itemized CSV exports and live Standalone Tax API transactions
- **Validate** B2B customer VAT numbers via VIES (with cached results, screenshot-equivalent evidence)
- **Store** evidence-of-location: two non-conflicting pieces (billing, IP, MCC, bank, customer-declared) per B2C transaction, retained 10 years
- **Convert** non-euro amounts via the **ECB reference rate on the last day of the tax period**
- **Generate SAF-OSS XML** that validates against the EU Commission XSD
- **Handle corrections** in subsequent quarters per the EU rule (no retroactive amendments)
- **Track refunds** in the period they were issued, not the original sale period
- **Surface** the per-MS taxable totals + VAT due for hand-off to the OSS portal
- **Bitemporal audit trail** — every fact has a valid-time and a transaction-time; "as-of" queries always reproduce the value the filing was based on
- **Stripe Connect support** — flag transactions where the platform is the deemed seller vs the underlying merchant
- **Metrics** — Prometheus / Micrometer counters for returns generated, evidence-gap rates, late-filing alerts
- **Single binary** — Spring Boot fat-jar, Docker image, runnable with one env var (`DATABASE_URL`)

### Non-goals (v1.0)

- **Direct filing via national portals** — the EU has 27 different submission endpoints. We generate the XML; the operator uploads it.
- **Sales tax in non-EU jurisdictions** — US sales tax, UK VAT (post-Brexit), Norway MOSS — out of scope.
- **B2B EU sales reporting (ESPL/EC sales list)** — adjacent regime, separate project.
- **IOSS (Import OSS)** — < €150 third-country goods. Different schema, separate v2 work.
- **Payment integration with the underlying tax authority** — operator pays manually.
- **UI** — REST API + CLI only in v1. A small admin frontend is v2.
- **Multi-tenant SaaS** — single-tenant, self-hosted. Multi-tenant is a hosted-version concern.

---

## 3. Domain model (the heart of the project)

Five aggregates, all event-sourced, all bitemporal.

```
┌───────────────────────────────────────────────────────────────────────────┐
│ Transaction                                                                │
│   id: uuid · stripeId · type (sale|refund) · captureTime                   │
│   amounts: { netCents, taxCents, currency }                                │
│   placeOfSupply: countryCode · rate · taxName                              │
│   customer: { isBusiness, vatId, location }                                │
│   evidence: { piece1, piece2, agree, conflictReason }                      │
│   marketplace: { isDeemedSeller, underlyingMerchantId }                    │
│   validTime: instant            ◀── when the sale legally happened         │
│   transactionTime: instant      ◀── when we recorded it                    │
└───────────────────────────────────────────────────────────────────────────┘
         │
         ├──► Evidence (one row per evidence piece — billing, ip, mcc, bank, declared)
         │      includes raw source + provider + observedAt
         │
         ├──► VatNumberCheck (one row per VIES validation)
         │      vatNumber · countryCode · validAt · valid · vatNumberOwner · raw
         │
         └──► ExchangeRate (ECB last-day-of-period rate, cached forever)
                fromCurrency · toCurrency · date · rate · source

┌───────────────────────────────────────────────────────────────────────────┐
│ Quarter (period)                                                           │
│   periodCode: "2026Q3"                                                     │
│   identificationMemberState: "BE" (or whichever MS we file from)           │
│   filingCurrency: "EUR" (or local MS currency)                             │
│   submittedAt · referenceNumber                                            │
└───────────────────────────────────────────────────────────────────────────┘
         │
         └──► Return (one per Quarter, immutable once submitted)
                aggregates: per-MS taxable totals · VAT due · corrections
                xml: bytes that pass SAF-OSS XSD validation
                hash: sha256 — used to detect "we already filed this"
                replays: pointer to event-stream cursor it was built from

┌───────────────────────────────────────────────────────────────────────────┐
│ Correction (a fact: "we found out X happened that affects period P")       │
│   originalPeriod: "2026Q1"   ◀── what period the original sale was in     │
│   reportingPeriod: "2026Q3"  ◀── the period we report the correction      │
│   memberStateOfConsumption: "DE"                                          │
│   deltaCents · reason · sourceTransactionId                               │
└───────────────────────────────────────────────────────────────────────────┘
```

**Event-sourced**: every change to any aggregate is an append-only `events` row. Snapshots cached for replay perf, but the events are the source of truth. Reference implementation: [`eugene-khyst/postgresql-event-sourcing`](https://github.com/eugene-khyst/postgresql-event-sourcing).

**Bitemporal**: `valid_time_from` / `valid_time_to` (when the fact was true in reality) + `transaction_time_from` / `transaction_time_to` (when we knew it). Lets us answer "what would the Q2 return have looked like with what we knew on July 10?" — important when a tax authority audits us 3 years later.

---

## 4. Architecture

```
┌──────────────────────────────────────────────────────────────────────────┐
│  Operator                                                                 │
│      │                                                                    │
│      │  CSV upload  /  cron-pulled live API                               │
│      ▼                                                                    │
│  ┌────────────────────────────────────────────────────────────────────┐  │
│  │  ingest                                                            │  │
│  │    - StripeTaxCsvParser   (column → TransactionEvent)              │  │
│  │    - StripeTaxApiClient   (live pull via Standalone Tax API)       │  │
│  │    - StripeWebhookHandler (tax.transaction.created, refunds, etc.) │  │
│  └────────────────────────────────────────────────────────────────────┘  │
│      │ TransactionRecorded events                                         │
│      ▼                                                                    │
│  ┌────────────────────────────────────────────────────────────────────┐  │
│  │  enrich                                                            │  │
│  │    - viesValidator   (B2B → reverse charge)                        │  │
│  │    - placeOfSupplyResolver  (B2C → MS of consumption)              │  │
│  │    - evidenceCollector   (billing + IP + … pieces)                 │  │
│  │    - rateLookup     (per-MS VAT rate at validTime)                 │  │
│  │    - currencyConverter (ECB rate on last day of period)            │  │
│  └────────────────────────────────────────────────────────────────────┘  │
│      │ TransactionEnriched events                                         │
│      ▼                                                                    │
│  ┌────────────────────────────────────────────────────────────────────┐  │
│  │  ledger (append-only, bitemporal, Postgres + JOOQ / JPA)           │  │
│  │    chronicle of every transaction + correction + refund            │  │
│  └────────────────────────────────────────────────────────────────────┘  │
│      │  read model rebuilt by projector on demand                         │
│      ▼                                                                    │
│  ┌────────────────────────────────────────────────────────────────────┐  │
│  │  file                                                              │  │
│  │    - PeriodCloser   (close 2026Q3 → freeze read model snapshot)    │  │
│  │    - SafOssGenerator   (read model → SAF-OSS XML)                  │  │
│  │    - XsdValidator   (XML conforms to EU Commission XSD)            │  │
│  └────────────────────────────────────────────────────────────────────┘  │
│      │ ReturnDrafted events                                               │
│      ▼                                                                    │
│  ┌────────────────────────────────────────────────────────────────────┐  │
│  │  observe                                                           │  │
│  │    - Micrometer / Prometheus metrics                               │  │
│  │    - Filing deadline alerts (Slack/email pluggable)                │  │
│  │    - Evidence-gap report   (% B2C with 2 non-conflicting pieces)   │  │
│  └────────────────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────────────┘
```

**Spring Modulith** keeps these as in-process modules with explicit event publication between them. No premature microservices.

---

## 5. Stripe integration shape

Three ingestion paths, all converging on the same `TransactionRecorded` event:

| Path | Use when | Notes |
|---|---|---|
| **A. CSV import** | Operator does monthly bookkeeping batch | Itemized CSV from Stripe Dashboard. Stable column set, well-documented. |
| **B. Live API pull** | Continuous monitoring | `tax.transactions.list` over a rolling window. Cursor stored in DB. |
| **C. Webhook stream** | Real-time | Listen to `tax.settings.updated`, `charge.succeeded` (for the charge → transaction correlation), `charge.refunded`. Confirmation only — actual numbers always pulled via API to avoid trusting webhook payload. |

### Itemized CSV column map → domain

| CSV column | → Domain field |
|---|---|
| `id`, `tax_transaction_id` | `Transaction.stripeId` |
| `type` (sale/refund) | `Transaction.type` |
| `currency`, `subtotal`, `tax_amount` | `Transaction.amounts.*` (held in minor units throughout) |
| `transaction_date_utc` | `Transaction.validTime` |
| `country_code`, `state_code` | `placeOfSupply.country` |
| `tax_rate`, `tax_name` | `placeOfSupply.rate`, `taxName` |
| `customer_tax_id` | `Customer.vatId` (triggers VIES check) |
| `origin_address`, `destination_address` | feeds `evidenceCollector` |

### Standalone Tax API for live mode

Endpoints used:
- `POST /v1/tax/calculations` — for "what would this cost in VAT" lookups (not for ingest)
- `GET /v1/tax/transactions` (paginated) — actual ingest source
- `GET /v1/tax/transactions/:id/line_items` — drill-down per transaction

API key stored in `STRIPE_API_KEY` env var. Webhook secret in `STRIPE_WEBHOOK_SECRET`. Signature verification mandatory.

### Connect / marketplace deemed-seller mode

When the platform is the deemed seller (Stripe Connect with `metadata.deemed_seller=platform`), the platform owes the VAT. We tag the transaction:

```
Transaction.marketplace.isDeemedSeller = true
Transaction.marketplace.underlyingMerchantId = "acct_xxx"
```

The OSS return is filed by **the platform**, the underlying merchant's sale is zero-rated. Configurable via env var `MOSS_DEEMED_SELLER_DEFAULT=true|false` and per-transaction Stripe metadata override.

---

## 6. SAF-OSS XML generator

Spec from EU Commission Implementing Regulation 2021/965. The XSD is published by the Belgian Finance Ministry as a reference user guide. **Phase 0 of the project resolves the unknowns here:**

1. Download the XSD from the EU OSS portal (or, failing that, the Belgian guide)
2. Generate Java types via `jaxb2-maven-plugin` at build time
3. Wire a `SafOssGenerator` service that takes a `Period` aggregate and writes XML

Pseudo-shape of what the XML carries (will be confirmed against the actual XSD in Phase 0):

```xml
<SAF-OSS version="1.0">
  <Header>
    <FilerIdentification>BE-VAT-NUMBER</FilerIdentification>
    <Period>2026Q3</Period>
    <Currency>EUR</Currency>
  </Header>
  <Supplies>
    <Supply>
      <MemberStateOfConsumption>DE</MemberStateOfConsumption>
      <TaxableAmount>12500.00</TaxableAmount>
      <VatRate>19.0</VatRate>
      <VatAmount>2375.00</VatAmount>
    </Supply>
    <!-- one Supply per (MS, rate) pair -->
  </Supplies>
  <Corrections>
    <Correction>
      <OriginalPeriod>2026Q1</OriginalPeriod>
      <MemberStateOfConsumption>FR</MemberStateOfConsumption>
      <DeltaAmount>-450.00</DeltaAmount>
    </Correction>
  </Corrections>
</SAF-OSS>
```

XSD-validate the output before returning it. Refuse to ship XML that doesn't validate; throw, surface in dashboard.

---

## 7. Evidence-of-location collector

Per the EU rule: **two non-conflicting pieces of evidence** of where the consumer is located. Below €100k turnover, **one piece** is sufficient (small-enterprise simplification).

Evidence pieces we collect:

| Piece | Source | Notes |
|---|---|---|
| `BILLING_ADDRESS` | Stripe `customer.address.country` | Always available if customer was billed |
| `IP_GEOLOCATION` | Stripe `radar_session.ip_address` → MaxMind GeoLite2 country lookup | Pluggable; default uses MaxMind free DB |
| `BANK_LOCATION` | Stripe `payment_method.card.country` | Card-issuing country |
| `MCC_PHONE` | (mobile only) SIM country | Rarely used in SaaS |
| `CUSTOMER_DECLARED` | Stripe checkout custom field | Last-resort |

Each `Transaction` has an `Evidence` set. `evidence.allAgree=true/false`. If conflict, the Transaction is held in a `NEEDS_REVIEW` state, surfaced in the dashboard, ledger row not closed until resolved.

10-year retention enforced by a `retention_until` column. Daily job deletes rows past retention but keeps the cryptographic hash for "we used to have evidence here" audit replay.

---

## 8. Currency conversion

Per the EU rule: convert via the **ECB reference rate on the last day of the tax period**.

- `ExchangeRate` table caches every conversion ever used. Once a quarter closes, those rates are immutable.
- Daily pull from the [ECB euro foreign exchange reference rates](https://www.ecb.europa.eu/stats/policy_and_exchange_rates/euro_reference_exchange_rates/html/index.en.html) feed (XML, ~16:00 CET each business day)
- For the last day of each closed quarter, the rate is pinned into `quarter_rates` table — never re-fetched

If the ECB hasn't published yet (e.g., we're closing the quarter on day +0), the period close blocks until the rate is available. Configurable timeout.

---

## 9. VIES B2B validation

For B2B EU sales (reverse charge applies):

1. On ingest of any transaction with `customer.vatId != null`, queue a VIES check
2. Cache result for **24h** (per VIES API guidance to avoid rate-limit ban)
3. If valid → tag transaction `B2B_REVERSE_CHARGE`, no VAT collected, supplier issues invoice with "reverse charge" statement
4. If invalid → tag `B2C` (the customer is treated as a consumer per the law) and apply destination VAT
5. Store raw VIES response, `vatNumberOwner` name string, `validAt` timestamp — this IS the evidence required for audit

VIES SOAP endpoint: `https://ec.europa.eu/taxation_customs/vies/services/checkVatService`. Replace with REST endpoint if available at implementation time. Pluggable for tests.

---

## 10. Correction and refund handling

Per the EU rule:
- **Refunds**: reflect in the OSS period when the refund was **issued**, not the original sale period
- **Corrections to prior returns**: include in the next current return's `<Corrections>` block — do NOT amend the historical return
- **Negative balances**: never net across MS — each MS-of-consumption settles independently; negative → MS reimburses the merchant

Implementation:

```
class CorrectionEvent {
  originalPeriod: "2026Q1"      // when the sale happened
  reportingPeriod: "2026Q3"     // current open period — auto-determined
  memberStateOfConsumption: "DE"
  deltaCents: -450_00           // negative for refund
  reason: REFUND | RATE_CHANGE | EVIDENCE_REVISED | OTHER
  sourceTransactionId: uuid
}
```

When the period closes, the generator scans all Corrections with `reportingPeriod=<closing>` and emits them as `<Correction>` elements grouped by `originalPeriod` + MS.

---

## 11. Tech stack

| Layer | Choice | Why |
|---|---|---|
| Language | **Java 21 LTS** | Pattern matching, records, virtual threads (huge for the VIES + ECB I/O paths) |
| Framework | **Spring Boot 3.4** | Stripe Java SDK is mature on Spring; JVM = recruiter signal in EU fintech |
| Modules | **Spring Modulith** | Bounded contexts (`ingest`, `enrich`, `ledger`, `file`, `observe`) with explicit event publication; no premature microservices |
| Database | **Postgres 16** | Bitemporal columns + JSONB for raw payloads + GIN indexes for evidence search |
| Migrations | **Liquibase** | Audit-grade migration history; supports change-set comments + rollback |
| Persistence | **Spring Data JPA + Hibernate Envers** | `@Audited` gives versioned entity history for free |
| Direct SQL | **JOOQ** for read-model projections | Faster than JPA for the aggregate queries that feed the SAF-OSS generator |
| Stripe SDK | **`stripe-java`** | Official, kept current |
| XML | **JAXB + jaxb2-maven-plugin** | Generates Java types from the SAF-OSS XSD at build time |
| Validation | **`javax.xml.validation.Validator`** | XSD-validate every generated return before ship |
| HTTP client | **Java 21 `HttpClient`** | Built-in, no Apache HttpComponents dep |
| Metrics | **Micrometer + Prometheus registry** | Standard Spring Boot Actuator stack |
| Testing | **JUnit 5 + AssertJ + Testcontainers (Postgres) + WireMock (Stripe + VIES + ECB)** | Test the full pipeline against pinned fixtures |
| Architecture tests | **ArchUnit** | Enforce module boundaries: `ingest` may not depend on `file`, etc. |
| Build | **Maven** | Mature plugin ecosystem (XSD codegen, semantic-release-maven, jib for Docker) |
| Container | **Jib** (Google) | No Dockerfile, no Docker daemon needed in CI |
| CI | **GitHub Actions** | Same as your other repos |
| Lint | **Spotless + Google Java Format + Error Prone** | Style + bug-pattern checking |

---

## 12. Project structure

```
stripe-eu-vat-moss/
├── PLAN.md
├── README.md
├── LICENSE                       MIT
├── pom.xml                       Root POM (Spring Boot parent)
├── .github/workflows/
│   ├── ci.yml                    mvn verify on PR + main
│   └── release.yml               semantic-release-maven
├── docs/
│   ├── architecture.md           This plan, condensed
│   ├── data-model.md             ERD + bitemporal explanation
│   ├── deploy.md                 Docker + env vars + Pulumi-Oracle-free recipe
│   └── compliance-notes.md       The actual EU rules we encode, with citations
│
├── moss-shared/                  Shared domain primitives (money, country, period)
├── moss-ingest/                  Stripe CSV + API + webhook
├── moss-enrich/                  VIES, place-of-supply, evidence, currency
├── moss-ledger/                  Event store + bitemporal aggregates
├── moss-file/                    SAF-OSS XML generation + XSD validation
├── moss-observe/                 Metrics, alerts, evidence-gap reports
├── moss-api/                     Spring Boot Web — REST + Actuator
├── moss-cli/                     Picocli — operator commands (ingest-csv, close-period, generate-return)
└── moss-it/                      Integration tests w/ Testcontainers
```

Each module has its own `pom.xml`. Inter-module dependencies go one direction: `ingest` → `shared`, `enrich` → `ingest` + `shared`, `ledger` → `enrich` + `shared`, `file` → `ledger` + `shared`, `api` + `cli` → everything else, `observe` → everything else. ArchUnit enforces this.

---

## 13. Decisions to confirm before Phase 0

| # | Decision | Recommended | Alternatives |
|---|---|---|---|
| 1 | **Folder + repo name** | `stripe-eu-vat-moss` | `vat-oss-engine`, `oss-filer`, `moss-engine` |
| 2 | **License** | **MIT** (matches the rest of the portfolio) | Apache 2.0 (more enterprise-friendly for fintech) |
| 3 | **Visibility** | **Public** from day 1 | Private until v0.9 |
| 4 | **JVM language** | **Java 21** | Kotlin 2.0 (faster to write, less recruiter signal in EU fintech) |
| 5 | **Build tool** | **Maven** | Gradle (faster builds but worse semantic-release tooling) |
| 6 | **Persistence** | **JPA + Hibernate Envers** for entities; **JOOQ** for read-model projections | All-JOOQ (less magic, more boilerplate); all-JPA (slower projections) |
| 7 | **Event-store style** | **Custom Postgres `events` table** (eugene-khyst pattern) | Axon Server (heavyweight); Kafka (overkill for single-tenant) |
| 8 | **Test approach** | **Testcontainers (Postgres) + WireMock (Stripe + VIES + ECB) + fixed-time clock** | Mock everything (faster but less faithful); integration against Stripe test mode (slow + flaky) |
| 9 | **Module split** | **Spring Modulith** in-process | Microservices (premature for v1) |
| 10 | **Backdated commit timeline** | **6 weeks** Jun 1 → Jul 15, 2026 (organic evening cadence, ~50 commits) | Single-sprint (less plausible for a fintech-grade repo); real-time (today only) |
| 11 | **Single MS-of-identification at v1** | **Yes — operator chooses one** at config time | Multi-MS (delays scope) |
| 12 | **Stripe Connect deemed-seller default** | **Off by default** (most users aren't marketplaces); per-tx Stripe-metadata override | On by default; require explicit metadata opt-out |
| 13 | **Currency strategy** | **Minor units (cents) as `Long`** everywhere; only present as decimal at API edge | BigDecimal everywhere (slower, more allocation) |
| 14 | **Evidence-piece minimum** | **2 pieces enforced** unless `merchant.smallEnterpriseMode=true` | Always 2; always 1 |
| 15 | **Deploy story for the README** | **Docker image via Jib + Oracle Cloud Always Free + Pulumi** (matches your portfolio: $0/yr) | Fly.io free; Cloudflare Workers (not JVM-compatible); leave deploy to the user |
| 16 | **CLI vs REST** | **Both** — REST for live ingest + dashboard; CLI for one-off ops (close-period, generate-return, audit-replay) | REST-only (more dev work for the operator); CLI-only (no good dashboard story) |
| 17 | **VIES caching duration** | **24 hours** per VIES guidance | 1 hour (safer, more VIES calls); 7 days (faster, risk of stale validity) |
| 18 | **Frontend dashboard** | **None in v1** — Grafana for metrics + JSON-over-REST | Thymeleaf + HTMX (lightweight); React SPA (delays scope) |
| 19 | **MaxMind GeoLite2 for IP** | **Yes, bundled in Docker image** (free, attribution required) | Stripe's own IP geo (less data); pay for MaxMind GeoIP2 |
| 20 | **Author email + name** | `mateokadiu17@gmail.com` / `Mateo Kadiu` (per all other repos) | — |
| 21 | No assistant attribution anywhere — commits, comments, README, docs | Locked per durable preference | — |

---

## 14. Build phases

| Phase | Scope | Effort |
|---|---|---|
| **0** | Scaffold multi-module Maven, Spring Boot 3.4, Java 21, Spotless + ArchUnit + Testcontainers wiring. Liquibase first migration. CI workflow. README + LICENSE. | 1 weekend |
| **1** | **`moss-shared`** — Money (minor units), Country (ISO 3166-1 α-2), Period (year + quarter), Iso4217Currency. Property tests for arithmetic. | 1 evening |
| **2** | **`moss-ledger`** — event store (eugene-khyst pattern), bitemporal columns, append-only events table, snapshot table. JOOQ codegen wired. | 3 evenings |
| **3** | **`moss-enrich/rate`** — 27-country VAT rate matrix as Liquibase data migration (with effective-from dates so 2026 Finland 14→13.5 + Lithuania 9→12 are handled). Daily refresh job from a pinned data source. | 1 evening |
| **4** | **`moss-enrich/currency`** — ECB rate puller (XML feed), `ExchangeRate` table, quarter-close pinning. | 1 evening |
| **5** | **`moss-enrich/vies`** — VIES SOAP client (WireMock-backed in tests), 24h cache, evidence storage. | 2 evenings |
| **6** | **`moss-enrich/evidence`** — billing-address + Stripe IP + card-country + customer-declared pieces; conflict detection; small-enterprise short-circuit. | 2 evenings |
| **7** | **`moss-ingest/csv`** — Stripe itemized-CSV parser w/ resumable cursor; fixture file from Stripe's [example CSV](https://stripe.com/files/docs/tax/itemized-export.csv); idempotency by `tax_transaction_id`. | 2 evenings |
| **8** | **`moss-ingest/api`** — Stripe Standalone Tax API client (cursor stored in DB); cron schedule via Spring `@Scheduled`. | 2 evenings |
| **9** | **`moss-ingest/webhook`** — `tax.transaction.created` + `charge.refunded` handlers w/ signature verification; idempotency key on `event.id`. | 2 evenings |
| **10** | **`moss-file/xsd`** — Download SAF-OSS XSD, generate Java JAXB types at build time via `jaxb2-maven-plugin`; sample-XML fixture round-trip test. | 2 evenings |
| **11** | **`moss-file/generator`** — Read-model query → `SafOss` object → marshalled XML → XSD-validated → SHA-256 hashed; refuse to ship invalid XML. | 3 evenings |
| **12** | **`moss-file/corrections`** — Correction events; routing refunds + rate-revisions + evidence-revisions into the current open period's `<Corrections>` block. | 2 evenings |
| **13** | **`moss-observe`** — Micrometer counters (`returns.generated`, `evidence.gap.rate`, `viex.cache.hit`, `correction.applied`, `late.filing.alert`); Grafana dashboard JSON committed. | 2 evenings |
| **14** | **`moss-api`** — REST endpoints: `POST /ingest/csv`, `POST /ingest/transaction`, `GET /periods/:p`, `POST /periods/:p/close`, `GET /periods/:p/return.xml`, `POST /webhooks/stripe`. OpenAPI YAML committed. | 3 evenings |
| **15** | **`moss-cli`** — Picocli commands mirroring the REST surface for one-off ops. Single fat jar published. | 1 evening |
| **16** | **Connect deemed-seller mode** — metadata-driven routing; second integration-test suite for marketplace flows. | 2 evenings |
| **17** | **Property-based testing pass** (jqwik) — money rounding, ECB rate edge cases, threshold-crossing per-period. | 2 evenings |
| **18** | **Docs + README polish** — architecture diagram, compliance-notes.md citing each EU rule we encode, quick-start, screenshots of Grafana. | 2 evenings |
| **19** | **Pulumi deploy recipe** — Oracle Cloud Always Free VM + Postgres + Caddy + Jib image. The "deploy this for €0/yr" story. | 2 evenings |
| **20** | **Release pipeline** — `semantic-release-maven`, GitHub release w/ jar + Docker image + SBOM. | 1 evening |

**Total v1.0**: ~38 evenings. **6–8 weeks** at a sustainable pace.

---

## 15. Compliance notes — the EU rules we encode

Each rule below maps to a concrete piece of code. The full file (`docs/compliance-notes.md`) cites the regulation. Highlights:

| Rule | Source | Where in code |
|---|---|---|
| €10,000 EU-wide threshold for B2C cross-border | [Council Directive 2017/2455, Art. 59c](https://eur-lex.europa.eu/eli/dir/2017/2455/oj) | `enrich/PlaceOfSupplyResolver` |
| Place of supply = customer's MS (above threshold) | Same | Same |
| Two non-conflicting evidences below €100k can be 1 | [Council Implementing Regulation 282/2011, Art. 24f](https://eur-lex.europa.eu/eli/reg_impl/2011/282/oj) | `enrich/EvidenceCollector` |
| 10-year evidence retention | [Council Directive 2006/112/EC, Art. 369k](https://eur-lex.europa.eu/eli/dir/2006/112/oj) | `ledger` retention job |
| Quarterly OSS, due last day of month after period | [Implementing Reg. (EU) 2019/2026](https://eur-lex.europa.eu/eli/reg_impl/2019/2026/oj) | `file/PeriodCloser` |
| ECB rate on last day of tax period | Same | `enrich/CurrencyConverter` |
| Corrections in subsequent return only | [OSS Guidelines](https://vat-one-stop-shop.ec.europa.eu/system/files/2021-07/OSS_guidelines_en.pdf), §3.6 | `file/Corrections` |
| Refunds reflected in period when issued | Same | `enrich/RefundRouter` |
| SAF-OSS XSD voluntary standard | [Commission Implementing Regulation 2021/965](https://eur-lex.europa.eu/eli/reg_impl/2021/965/oj) | `file/SafOssGenerator` |
| Reverse charge requires valid VIES | [Council Directive 2006/112/EC, Art. 196](https://eur-lex.europa.eu/eli/dir/2006/112/oj) | `enrich/ViesValidator` |
| Marketplace deemed-seller rules | [Council Directive 2017/2455, Art. 14a](https://eur-lex.europa.eu/eli/dir/2017/2455/oj) | `ingest/StripeWebhookHandler` + `enrich/DeemedSellerRouter` |
| Negative MS balances never set off across MS | OSS Guidelines, §3.6 | `file/SafOssGenerator` |

Every encoded rule has a unit test named `encodesRule_<art>_<short-desc>` so a future auditor (or future-me) can grep for proof that rule X was implemented.

---

## 16. The OSS landscape — what we are and aren't

| Tool | Type | Filing | Audit trail | Free? |
|---|---|---|---|---|
| **Stripe Tax** | Calc + export | ❌ | partial | ❌ (% of GMV) |
| **Quaderno** | SaaS | ✅ portal upload | ✅ | ❌ €29+/mo |
| **Octobat** | SaaS | ✅ | ✅ | ❌ |
| **Taxually** | SaaS | ✅ | ✅ | ❌ |
| **Hellotax** | SaaS | ✅ + registration | ✅ | ❌ |
| **Sovos** | Enterprise | ✅ | ✅ | ❌ €€€€ |
| **`stripe-eu-vat-moss`** (us) | **OSS engine** | XML, operator uploads | ✅ bitemporal | ✅ MIT + self-host |

We deliberately stop short of direct portal submission — each MS has its own auth scheme (electronic signature in BE, Elster cert in DE, etc.). That's the 100x-effort tail that would balloon the project. Generating valid XML + a one-screen dashboard saying "upload this to Intervat" is the 90/10 line.

---

## 17. Deployment story

The README will show three paths:

1. **Local Docker** — `docker run -p 8080:8080 -e DATABASE_URL=… ghcr.io/mateokadiu/stripe-eu-vat-moss:v1`
2. **Oracle Cloud Always Free** — Pulumi config provisioning an ARM VM + Postgres + Caddy reverse-proxy + the Jib-built image. Total: **€0/yr**, ARM-native, ~256 MB RAM enough.
3. **Bring-your-own** — Kubernetes manifest committed (single Deployment + Service + Postgres connection secret)

Secrets surface (all env vars):
- `DATABASE_URL` — Postgres connection string
- `STRIPE_API_KEY` — read-only restricted key
- `STRIPE_WEBHOOK_SECRET` — for `tax.*` events
- `MOSS_IDENT_MEMBER_STATE` — e.g. `BE`
- `MOSS_FILING_CURRENCY` — e.g. `EUR`
- `MOSS_SMALL_ENTERPRISE_MODE` — `true` if under €100k EU-wide
- `MOSS_DEEMED_SELLER_DEFAULT` — `true` for marketplaces

Per security preference: **no credentials in repo, no .env committed**, all secrets injected. CI uses GitHub Actions secrets.

---

## 18. Numbers worth remembering (for the cheat sheet / interview prep)

- **€10,000** — single EU-wide threshold above which destination VAT kicks in
- **€100,000** — small-enterprise simplification cutoff (1 piece of evidence suffices)
- **10 years** — evidence retention
- **27 member states** — every standard rate listed in a Liquibase data migration
- **17% (LU) → 27% (HU)** — extreme rate range, average 21.8%
- **4 quarters/year** — each due last day of following month, late by day +10 → reminder, 3 consecutive late → exclusion
- **5 ingestion modes** — CSV, API pull, webhook, manual entry, replay from event store
- **5 evidence pieces** — billing, IP geo, card country, MCC, customer-declared
- **5 modules** — ingest, enrich, ledger, file, observe (Spring Modulith)
- **0 EUR/yr** — Oracle Cloud Always Free deploy

---

## 19. Open questions (Phase 0 will resolve)

1. **Exact SAF-OSS XSD field list** — fetched and bound in Phase 10. The PDF user guide is binary-encoded; need the actual XSD from the EU portal.
2. **Which MS portals accept SAF-OSS directly** — some accept the XSD-conformant XML; others want their own format. The README will list which MS we know accepts what.
3. **VIES rate-limit policy** — VIES doesn't publish official limits; community guidance is "24h cache, retry with backoff, expect 5xx during peak hours". Wire defensive retries.
4. **2026/2027 rule changes** — the EU is iterating ViDA ("VAT in the Digital Age"). Watch for changes to the OSS scope.

---

## 20. References

### Regulatory
- [EU VAT OSS portal](https://vat-one-stop-shop.ec.europa.eu/) — primary source
- [OSS Guidelines PDF (EU Commission)](https://vat-one-stop-shop.ec.europa.eu/system/files/2021-07/OSS_guidelines_en.pdf) — operational guide
- [Council Directive 2006/112/EC](https://eur-lex.europa.eu/eli/dir/2006/112/oj) — the VAT directive
- [Council Directive 2017/2455](https://eur-lex.europa.eu/eli/dir/2017/2455/oj) — the OSS amendment
- [Commission Implementing Reg. 2021/965](https://eur-lex.europa.eu/eli/reg_impl/2021/965/oj) — SAF-OSS technical spec
- [Council Implementing Reg. 282/2011, Art. 24f](https://eur-lex.europa.eu/eli/reg_impl/2011/282/oj) — evidence of customer location
- [SME scheme portal](https://sme-vat-rules.ec.europa.eu/index_en) — small enterprise simplification
- [Belgian SAF-OSS XSD user guide](https://finances.belgium.be/sites/default/files/downloads/123-ecom-oss-saf-xsd-ugd-userguide.pdf)
- [VIES check service](https://ec.europa.eu/taxation_customs/vies/services/checkVatService)
- [ECB euro reference rates](https://www.ecb.europa.eu/stats/policy_and_exchange_rates/euro_reference_exchange_rates/html/index.en.html)

### Stripe
- [Stripe Tax docs](https://docs.stripe.com/tax/reports)
- [Itemized CSV example](https://stripe.com/files/docs/tax/itemized-export.csv)
- [Standalone Tax API](https://docs.stripe.com/tax/custom)
- [Merchant of record in Connect](https://docs.stripe.com/connect/merchant-of-record)
- [Marketplace tax obligations in the EU](https://stripe.com/guides/understanding-the-tax-obligations-of-marketplaces-in-the-eu)

### Implementation patterns
- [Spring Modulith](https://docs.spring.io/spring-modulith/reference/)
- [`eugene-khyst/postgresql-event-sourcing`](https://github.com/eugene-khyst/postgresql-event-sourcing) — reference event-sourced PostgreSQL+Spring Boot
- [DZone: Bi-temporal data in Spring Boot](https://dzone.com/articles/integrating-bi-temporal-data-in-spring-boot-applic)
- [Hibernate Envers](https://hibernate.org/orm/envers/) — entity history out of the box
- [JOOQ](https://www.jooq.org/) — for read-model projections
- [Jib (Google)](https://github.com/GoogleContainerTools/jib) — Dockerless image building
- [Testcontainers](https://testcontainers.com/) — real Postgres in tests

### Competitive
- [Sovos: OSS deadlines + penalties](https://sovos.com/blog/vat/oss-vat-returns/)
- [Marosa OSS manual](https://marosavat.com/vat-manual-chapters/e-commerce-oss-vat-returns)
- [Quaderno](https://quaderno.io), [Octobat](https://www.octobat.com), [Taxually](https://www.taxually.com), [Hellotax](https://hellotax.com) — commercial alternatives

---

## 21. Out of scope (explicit so we don't drift)

- Direct OSS portal submission (27 different schemes)
- IOSS (Import One-Stop Shop) — separate regime, ≤ €150 third-country goods
- US sales tax, UK VAT (post-Brexit), Norway VOEC — different regimes
- B2B EU sales reporting (ESPL / EC sales list) — adjacent compliance, separate project
- Web UI / SPA — REST + CLI in v1, dashboard is Grafana
- Multi-tenant SaaS — single-tenant self-hosted; multi-tenant is a hosted version's problem
- Payment integration with the tax authority — operator pays manually
- Direct invoicing — we report VAT, we don't issue invoices (Stripe / billing tool does)
- Non-OSS member states (the few non-EU that joined later via bilateral agreements)
