package io.github.mateokadiu.moss.infra;

/**
 * Pulumi-Java stack for the Oracle Cloud Always Free deployment.
 *
 * <p>Resources provisioned:
 *
 * <ul>
 *   <li>VCN + public subnet + internet gateway + route table
 *   <li>Ampere A1 ARM compute (1 OCPU / 6 GB — free tier) running the moss image
 *   <li>Sibling compute running Postgres 16 (also free tier)
 *   <li>Caddy reverse-proxy with automatic TLS on a user-supplied domain
 * </ul>
 *
 * <p>Secrets are read from Pulumi configuration ({@code pulumi config set --secret stripe.api}).
 * Nothing sensitive is committed.
 *
 * <p>This file uses {@link com.pulumi.Pulumi} as a thin entrypoint; the actual resource graph
 * lives in {@link OciOrchestrator}.
 */
public final class MossStack {

  private MossStack() {}

  public static void main(String[] args) {
    com.pulumi.Pulumi.run(
        ctx -> {
          var orchestrator = new OciOrchestrator(ctx);
          orchestrator.provision();
        });
  }
}
