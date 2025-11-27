package io.github.mateokadiu.moss.observe;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

class MossMetricsTest {

  @Test
  void incrementsCountersIntoRegistry() {
    var registry = new SimpleMeterRegistry();
    var metrics = new MossMetrics(registry);

    metrics.returnsGenerated.increment();
    metrics.correctionsApplied.increment(3);
    metrics.viesCacheHits.increment();

    assertThat(registry.find("moss_returns_generated_total").counter().count()).isEqualTo(1.0);
    assertThat(registry.find("moss_corrections_applied_total").counter().count()).isEqualTo(3.0);
    assertThat(registry.find("moss_vies_cache_hits_total").counter().count()).isEqualTo(1.0);
  }

  @Test
  void dualRegistryAcceptsBothPathsForSameSeries() {
    var dual = new DualRegistry();
    var metrics = new MossMetrics(dual.registry());

    metrics.returnsGenerated.increment(5);

    // Both registries see the same count
    assertThat(dual.prometheus().find("moss_returns_generated_total").counter().count())
        .isEqualTo(5.0);
    assertThat(dual.otlp().find("moss_returns_generated_total").counter().count()).isEqualTo(5.0);
  }
}
