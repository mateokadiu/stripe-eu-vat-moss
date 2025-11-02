# SAF-OSS schema — implementation note

The Commission Implementing Regulation (EU) 2021/965 of 9 June 2021 defines a
voluntary "Standard Audit File for Tax — One-Stop-Shop" (SAF-OSS). The technical
artefacts (XSD, code lists, user guide) are published by the Belgian Finance
Ministry as the reference national implementation —
https://finances.belgium.be/sites/default/files/downloads/123-ecom-oss-saf-xsd-ugd-userguide.pdf —
but the XSD itself is gated behind portal-access registration.

Rather than vendor a third-party XSD we cannot redistribute, we encode the
schema's data model as a set of JAXB-annotated Java records that mirror the
fields enumerated in Article 2 and Annexes I and II of the Implementing
Regulation. The resulting XML is structurally compliant; XSD-validated bytes are
emitted only when an operator-supplied XSD is wired in via the `SafOssValidator`
hook.

References
----------
- Commission Implementing Regulation (EU) 2021/965, Articles 2 and Annexes I/II
- OSS Guidelines (EU Commission, July 2021), section 3.6 on corrections
- Belgian Finance Ministry SAF-OSS user guide (linked above)

The structural elements we emit follow the schema's macro shape: a
`SAF-OSS` envelope with a single `Header`, zero-or-more `Supply` rows grouped
by member state of consumption + VAT rate, and zero-or-more `Correction` rows
covering refunds and rate revisions that affect earlier filing periods.
