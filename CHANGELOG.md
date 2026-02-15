# Changelog

## v0.1.0 — 2026-02-15

First release.

- 27-country EU VAT rate matrix as Liquibase data, with 2026 changes pre-loaded
- ECB-pinned currency conversion at quarter close
- VIES SOAP client with 24h cache and raw-response audit retention
- Five evidence-piece types with conflict detection and 10y retention
- Append-only event store with bitemporal "as-of" replay
- SAF-OSS JAXB generator with optional XSD validation gate
- Refund routing into the period when issued
- Stripe Connect deemed-seller mode via metadata override
- REST endpoints + OpenAPI yaml committed
- Picocli CLI mirroring the REST surface
- Micrometer dual registry (Prometheus + OTLP) + Grafana dashboard JSON
- Jib + distroless base image
- CycloneDX SBOM in the release pipeline
- Pulumi-Java recipe for Oracle Cloud Always Free deploy (0 EUR/yr)
