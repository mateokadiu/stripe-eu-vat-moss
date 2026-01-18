# stripe-eu-vat-moss

[![CI](https://github.com/mateokadiu/stripe-eu-vat-moss/actions/workflows/ci.yml/badge.svg)](https://github.com/mateokadiu/stripe-eu-vat-moss/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java](https://img.shields.io/badge/Java-21-blue)](#)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4-brightgreen)](#)

An open-source EU VAT One-Stop-Shop automation engine. Sits next to a
Stripe Tax integration and fills the last mile that Stripe stops at:
file-ready SAF-OSS XML returns, evidence-of-location storage, VIES B2B
validation, refund and correction handling, ECB currency conversion, and
marketplace deemed-seller routing — with a bitemporal audit trail.

Free, MIT, self-hostable.

## Why this exists

Every EU SaaS shop crossing the EUR 10,000 cross-border B2C threshold owes
quarterly OSS VAT returns in the Member State of identification. Stripe Tax
calculates per-line VAT correctly but does not file the return. The last
mile is left to merchants who either hand-key a quarterly summary into the
national portal, or pay a commercial SaaS for it.

`stripe-eu-vat-moss` is the missing engine: it ingests Stripe Tax data
(itemized CSV exports or live Standalone Tax API), enriches it with the EU
rules an auditor checks for, and emits SAF-OSS XML the operator uploads.

## What's in the box

```
+---------+   +---------+   +----------+   +--------+   +---------+
| ingest  |-->| enrich  |-->| ledger   |-->| file   |-->| observe |
|         |   |         |   | (events) |   |        |   |         |
| CSV     |   | VIES    |   | bitemp.  |   | SAF-OSS|   | Prom +  |
| API     |   | ECB     |   | Postgres |   | XML +  |   | OTel    |
| webhook |   | evidence|   | JOOQ     |   | XSD    |   | Grafana |
+---------+   +---------+   +----------+   +--------+   +---------+
```

- 27-country VAT rate matrix as Liquibase data, with 2026 changes pre-loaded
- ECB-pinned currency conversion (last day of period)
- VIES SOAP client with 24h cache + raw-response retention for audit
- Five evidence-piece types with conflict detection + 10y retention
- Append-only event store with bitemporal "as-of" replay
- SAF-OSS JAXB generator with XSD validation gate + sha-256 hashing
- Refund routing into the period when issued, never retroactive
- Stripe Connect deemed-seller mode (Art. 14a) via metadata override
- Picocli CLI mirroring the REST surface
- Micrometer dual registry (Prometheus + OTLP) + Grafana dashboard JSON

## Quick start

```bash
./gradlew check assemble
./gradlew :moss-api:bootRun
```

```bash
curl -X POST http://localhost:8080/ingest/csv \
  -H "Content-Type: text/csv" \
  --data-binary @itemized-export.csv
```

```bash
curl http://localhost:8080/periods/2026Q3
curl -X POST http://localhost:8080/periods/2026Q3/close
curl http://localhost:8080/periods/2026Q3/return.xml -o return.xml
```

## CLI

```bash
moss ingest-csv --file itemized-export.csv
moss close-period 2026Q3
moss generate-return 2026Q3 --out return.xml
moss audit-replay --tx <uuid> --as-of 2026-07-15T10:00:00Z
```

## Deploy

See `docs/deploy.md`. Three documented paths: local Docker, Oracle Cloud
Always Free via Pulumi-Java (0 EUR/yr), and a stock Kubernetes manifest.

## Docs

- `docs/architecture.md` — bounded contexts + bitemporal model
- `docs/compliance-notes.md` — which EU rule maps to which file
- `docs/saf-oss-notes.md` — implementation notes on the SAF-OSS XSD
- `docs/deploy.md` — three deployment recipes
- `docs/openapi.yaml` — REST surface, committed

## License

MIT. Copyright (c) 2026 Mateo Kadiu. See `LICENSE`.
