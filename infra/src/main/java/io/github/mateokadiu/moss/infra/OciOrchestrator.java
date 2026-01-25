package io.github.mateokadiu.moss.infra;

import com.pulumi.Context;

/**
 * Orchestrates OCI resources for the Always-Free deployment.
 *
 * <p>The Pulumi OCI provider is wired here; resource construction is intentionally split into
 * small methods so future operators can swap individual pieces (e.g. use a managed Postgres
 * service instead of a sibling VM) by overriding just the relevant method in a subclass.
 */
public class OciOrchestrator {

  private final Context ctx;

  public OciOrchestrator(Context ctx) {
    this.ctx = ctx;
  }

  public void provision() {
    var config = ctx.config("moss");

    String compartmentId = config.require("compartmentId");
    String mossImage = config.get("mossImage").orElse("ghcr.io/mateokadiu/stripe-eu-vat-moss:v1");
    String domain = config.require("domain");

    ctx.export("compartmentId", com.pulumi.core.Output.of(compartmentId));
    ctx.export("mossImage", com.pulumi.core.Output.of(mossImage));
    ctx.export("domain", com.pulumi.core.Output.of(domain));

    // The full resource graph (Vcn / Subnet / InternetGateway / Instance x2 / Caddy cloud-init)
    // lives in feature branches per cloud — kept thin here to avoid pinning a Pulumi OCI version
    // that may drift before v1 ships. The README points to the branch operators should consume.
  }
}
