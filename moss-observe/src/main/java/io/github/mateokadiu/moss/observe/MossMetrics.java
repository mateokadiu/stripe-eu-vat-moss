package io.github.mateokadiu.moss.observe;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Objects;

/**
 * Domain-specific Micrometer counters and summaries. Names follow Prometheus convention: {@code
 * moss_<subsystem>_<unit>_<suffix>}, all snake-case.
 */
public final class MossMetrics {

  public final Counter returnsGenerated;
  public final Counter correctionsApplied;
  public final Counter lateFilingAlerts;
  public final Counter viesCacheHits;
  public final Counter viesUpstreamCalls;
  public final Counter evidenceGapDetected;
  public final DistributionSummary returnsTaxableAmountEur;

  public MossMetrics(MeterRegistry registry) {
    Objects.requireNonNull(registry, "registry");
    this.returnsGenerated =
        Counter.builder("moss_returns_generated_total")
            .description("Count of SAF-OSS returns generated")
            .register(registry);
    this.correctionsApplied =
        Counter.builder("moss_corrections_applied_total")
            .description("Count of correction rows emitted")
            .register(registry);
    this.lateFilingAlerts =
        Counter.builder("moss_late_filing_alerts_total")
            .description("Count of late-filing alerts fired")
            .register(registry);
    this.viesCacheHits =
        Counter.builder("moss_vies_cache_hits_total")
            .description("Count of VIES checks served from cache")
            .register(registry);
    this.viesUpstreamCalls =
        Counter.builder("moss_vies_upstream_calls_total")
            .description("Count of VIES checks that hit the upstream service")
            .register(registry);
    this.evidenceGapDetected =
        Counter.builder("moss_evidence_gap_detected_total")
            .description("Count of transactions held due to insufficient or conflicting evidence")
            .register(registry);
    this.returnsTaxableAmountEur =
        DistributionSummary.builder("moss_returns_taxable_amount_eur")
            .baseUnit("EUR")
            .description("Taxable amount per generated return (EUR)")
            .register(registry);
  }
}
