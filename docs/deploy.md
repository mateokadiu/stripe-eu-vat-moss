# Deployment

Three documented paths.

## 1. Local Docker

```bash
./gradlew :moss-api:jibDockerBuild
docker run --rm \
  -p 8080:8080 \
  -e DATABASE_URL=jdbc:postgresql://host.docker.internal:5432/moss \
  -e STRIPE_API_KEY=sk_test_... \
  -e STRIPE_WEBHOOK_SECRET=whsec_... \
  -e MOSS_IDENT_MEMBER_STATE=BE \
  -e MOSS_FILING_CURRENCY=EUR \
  ghcr.io/mateokadiu/stripe-eu-vat-moss:v1
```

## 2. Oracle Cloud Always Free (Pulumi-Java)

See `infra/` (added in phase 19) — an Ampere ARM A1 VM, a 1 OCPU /
12 GB shape (free tier), Postgres 16 in a sibling VM, and Caddy
reverse-proxying TLS. Total cost: 0 EUR/yr.

```bash
cd infra
pulumi up
```

The Pulumi program provisions the VM, configures cloud-init to fetch
the image, runs Liquibase migrations on first boot, and registers a
healthcheck. Secrets are stored in Pulumi configuration; nothing
sensitive is committed.

## 3. Kubernetes

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: stripe-eu-vat-moss
spec:
  replicas: 1
  selector:
    matchLabels: { app: moss }
  template:
    metadata: { labels: { app: moss } }
    spec:
      containers:
        - name: moss
          image: ghcr.io/mateokadiu/stripe-eu-vat-moss:v1
          env:
            - name: DATABASE_URL
              valueFrom: { secretKeyRef: { name: moss, key: db-url } }
            - name: STRIPE_API_KEY
              valueFrom: { secretKeyRef: { name: moss, key: stripe-key } }
            - name: STRIPE_WEBHOOK_SECRET
              valueFrom: { secretKeyRef: { name: moss, key: webhook } }
          ports:
            - containerPort: 8080
          readinessProbe:
            httpGet: { path: /actuator/health, port: 8080 }
```

## Environment variables

| Name | Purpose |
|---|---|
| `DATABASE_URL` | JDBC URL for Postgres |
| `STRIPE_API_KEY` | Restricted-key read access to the Tax API |
| `STRIPE_WEBHOOK_SECRET` | Signing secret for the webhook receiver |
| `MOSS_IDENT_MEMBER_STATE` | e.g. BE, DE, IE |
| `MOSS_FILING_CURRENCY` | usually EUR |
| `MOSS_SMALL_ENTERPRISE_MODE` | true to allow 1 evidence piece |
| `MOSS_DEEMED_SELLER_DEFAULT` | true for marketplaces filing on behalf of underlying merchants |
