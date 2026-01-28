# infra/

Pulumi-Java stack provisioning the Oracle Cloud Always Free deployment for
`stripe-eu-vat-moss`.

## Prerequisites

- Pulumi CLI 3.x
- An OCI tenancy with the Always Free entitlement
- A configured Pulumi stack: `pulumi stack init prod`
- Compartment OCID, region, and a domain you control

## Configuration

```bash
pulumi config set moss:compartmentId <ocid>
pulumi config set moss:domain moss.example.com
pulumi config set --secret stripe:apiKey <sk_live_...>
pulumi config set --secret stripe:webhookSecret <whsec_...>
```

## Bring up

```bash
pulumi up
```

The stack provisions:

- A VCN with a public subnet and internet gateway
- An Ampere A1 ARM compute instance (1 OCPU / 6 GB — free tier) running the
  moss container
- A sibling compute running Postgres 16
- A Caddy reverse-proxy with automatic TLS

Total cost: 0 EUR/yr.

## Tear down

```bash
pulumi destroy
```
