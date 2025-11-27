package io.github.mateokadiu.moss.observe;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import io.micrometer.registry.otlp.OtlpConfig;
import io.micrometer.registry.otlp.OtlpMeterRegistry;

/**
 * Factory for a Micrometer {@link CompositeMeterRegistry} that fans metrics out to both a
 * Prometheus scrape endpoint and an OTLP exporter. Operators pick which sink to consume — both
 * collect the same series.
 */
public final class DualRegistry {

  private final PrometheusMeterRegistry prometheus;
  private final OtlpMeterRegistry otlp;
  private final CompositeMeterRegistry composite;

  public DualRegistry() {
    this.prometheus = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
    this.otlp = new OtlpMeterRegistry(OtlpConfig.DEFAULT, Clock.SYSTEM);
    this.composite = new CompositeMeterRegistry();
    this.composite.add(prometheus);
    this.composite.add(otlp);
  }

  public CompositeMeterRegistry registry() {
    return composite;
  }

  public PrometheusMeterRegistry prometheus() {
    return prometheus;
  }

  public OtlpMeterRegistry otlp() {
    return otlp;
  }
}
