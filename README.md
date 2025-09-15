# stripe-eu-vat-moss

[![CI](https://github.com/mateokadiu/stripe-eu-vat-moss/actions/workflows/ci.yml/badge.svg)](https://github.com/mateokadiu/stripe-eu-vat-moss/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java](https://img.shields.io/badge/Java-21-blue)](#)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4-brightgreen)](#)

An open-source EU VAT One-Stop-Shop automation engine. It sits next to a
Stripe Tax integration and fills the last mile that Stripe stops at: file-ready
SAF-OSS XML returns, evidence-of-location storage, VIES B2B validation,
refund and correction handling, ECB currency conversion, and marketplace
deemed-seller routing — all with a bitemporal audit trail.

Free, MIT, self-hostable.

## What it is

Every EU SaaS shop crossing the EUR 10,000 cross-border B2C threshold owes
quarterly OSS VAT returns in its Member State of identification. Stripe Tax
calculates per-line VAT correctly but does not file the return. The "last mile"
is left to merchants who either hand-key a quarterly summary into the national
portal, or pay a commercial SaaS for it.

`stripe-eu-vat-moss` is the missing engine: it ingests Stripe Tax data
(itemized CSV exports or live Standalone Tax API), enriches it with the EU
rules an auditor will check for, and emits SAF-OSS XML the operator can upload.

## Layout

- `moss-shared` — domain primitives (Money, Country, Period, Currency)
- `moss-ledger` — Postgres event store, bitemporal aggregates
- `moss-enrich` — VIES, place-of-supply, evidence, currency conversion
- `moss-ingest` — Stripe CSV, Standalone Tax API, webhook ingest
- `moss-file` — SAF-OSS XML generator + XSD validation
- `moss-observe` — Micrometer metrics + alerts
- `moss-api` — Spring Boot REST + Actuator
- `moss-cli` — Picocli for one-off ops
- `moss-it` — cross-module integration tests

## Quick start

```bash
./gradlew check assemble
./gradlew :moss-api:bootRun
```

Then point a Stripe Tax CSV at the ingest endpoint:

```bash
curl -X POST http://localhost:8080/ingest/csv \
  -H "Content-Type: text/csv" \
  --data-binary @itemized-export.csv
```

## Deploy

Three documented paths in `docs/deploy.md`:

1. Local Docker (Jib-built image, distroless base)
2. Oracle Cloud Always Free (Pulumi-Java recipe, 0 EUR/yr)
3. Bring-your-own Kubernetes

## Status

v0.1 in active development. See `PLAN.md` for the implementation roadmap.

## License

MIT. Copyright (c) 2026 Mateo Kadiu. See `LICENSE`.
