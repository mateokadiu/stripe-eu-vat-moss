# Compliance notes

The rules below are encoded in the source. Each row maps to a concrete file +
test; an auditor (or future-me) can grep for `encodesRule_` to find proof.

| Rule | Source | Where in code |
|---|---|---|
| EUR 10,000 EU-wide threshold for B2C cross-border | Council Directive 2017/2455, Art. 59c | `moss-enrich/.../place/PlaceOfSupplyResolver.java` |
| Place of supply = customer's MS above threshold | Council Directive 2017/2455, Art. 59c | same |
| Two non-conflicting evidences (one below EUR 100k) | Council Implementing Reg. 282/2011, Art. 24f | `moss-enrich/.../evidence/EvidenceBundle.java` |
| 10-year evidence retention | Council Directive 2006/112/EC, Art. 369k | `moss-enrich/.../evidence/EvidenceCollector.retentionUntil` |
| Quarterly OSS, due last day of month after period | Implementing Reg. (EU) 2019/2026 | `moss-shared/.../Period.filingDeadline()` |
| ECB rate on last day of tax period | Implementing Reg. (EU) 2019/2026 | `moss-enrich/.../currency/EcbCurrencyConverter.java` |
| Corrections in subsequent return only | OSS Guidelines, sec. 3.6 | `moss-file/.../corrections/CorrectionsRouter.java` |
| Refunds reflected in period when issued | OSS Guidelines, sec. 3.6 | same |
| SAF-OSS XSD voluntary standard | Commission Implementing Reg. 2021/965 | `moss-file/.../saf/SafOss.java` |
| Reverse charge requires valid VIES | Council Directive 2006/112/EC, Art. 196 | `moss-enrich/.../vies/CachedViesValidator.java` |
| Marketplace deemed-seller rules | Council Directive 2017/2455, Art. 14a | `moss-enrich/.../marketplace/DeemedSellerRouter.java` |
| Negative MS balances never set off across MS | OSS Guidelines, sec. 3.6 | `moss-file/.../generator/SafOssGenerator.java` |

## Threshold (Art. 59c)

The EUR 10,000 cross-border B2C threshold is applied year-by-year, with a
running total carried in the supplier's filing currency. Once crossed in a
given year, the supplier files OSS for every subsequent in-year cross-border
transaction in that calendar year.

Below the threshold, the supplier-MS rules apply. The threshold is checked
inside `PlaceOfSupplyResolver.resolve`; the running total is held in the
read-model projection and fed to the resolver as a `Money` parameter.

## Evidence (Art. 24f)

Two non-conflicting pieces of evidence are required for every B2C electronic
supply taxed in the EU. A "piece" is a row in `evidence_pieces` whose type
falls into one of `BILLING_ADDRESS`, `IP_GEOLOCATION`, `BANK_LOCATION`,
`MCC_PHONE`, `CUSTOMER_DECLARED`. Two pieces of the same type do not count as
two distinct pieces — the resolver enforces this with a distinct-type check.

Operators below EUR 100k EU-wide annual turnover can set
`MOSS_SMALL_ENTERPRISE_MODE=true` to relax this to one piece.

## Retention (Art. 369k)

Each evidence row stores a `retention_until` date computed as
`observed_at + 10 years`. A daily job (`RetentionSweeper`) purges rows past
their retention; the row's `source_hash` (sha-256 of the raw source) is kept
forever in a separate audit log to prove that evidence existed at filing
time, even after deletion.

## Currency (Implementing Reg. 2019/2026)

ECB euro reference rates are pulled daily from the published XML feed. When a
period closes, the last-day rate for every (foreign currency -> filing
currency) pair seen in that period is pinned in `quarter_rates`. The
`EcbCurrencyConverter` refuses to convert closed-period amounts against any
rate other than the pinned one — replays therefore always yield the same
number.

## Corrections (OSS Guidelines, sec. 3.6)

Refunds are reported in the period they were issued, not the original sale
period. Prior-period rate or evidence revisions go into the next current
return's `<Corrections>` block — the historical return is never amended.

`CorrectionsRouter.route` returns `Optional.empty()` for same-period
adjustments (the caller should adjust the original line instead) and a
`CorrectionEvent` for cross-period ones, with the routing computed from the
operator's wall clock.

## SAF-OSS (Implementing Reg. 2021/965)

The SAF-OSS XML is built by `SafOssGenerator` from the read-model and
marshalled via JAXB. When an operator-supplied XSD is registered with
`SafOssMarshaller.withXsd`, the marshaller validates before returning bytes;
an invalid document never leaves the marshaller.

## Reverse charge (Art. 196)

For B2B sales to a VIES-validated VAT number, the supplier collects no VAT
and the reverse-charge mechanism applies. The validation is cached for 24
hours per VIES guidance; the raw response is retained as audit evidence.

## Marketplace deemed seller (Art. 14a)

When a Stripe Connect platform is the deemed seller, the platform owes the
OSS VAT for the transaction. The platform is identified per-transaction by
the `deemed_seller=platform` metadata key on the charge, with an
operator-level default via `MOSS_DEEMED_SELLER_DEFAULT`.
