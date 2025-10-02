package io.github.mateokadiu.moss.enrich.vies;

import static org.assertj.core.api.Assertions.assertThat;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.github.mateokadiu.moss.ledger.Migrator;
import io.github.mateokadiu.moss.shared.Country;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class CachedViesValidatorIT {

  private static PostgreSQLContainer<?> pg;
  private static HikariDataSource ds;

  @BeforeAll
  static void setup() {
    pg = new PostgreSQLContainer<>("postgres:16-alpine");
    pg.start();
    HikariConfig cfg = new HikariConfig();
    cfg.setJdbcUrl(pg.getJdbcUrl());
    cfg.setUsername(pg.getUsername());
    cfg.setPassword(pg.getPassword());
    ds = new HikariDataSource(cfg);
    new Migrator(ds).migrate();
  }

  @AfterAll
  static void teardown() {
    if (ds != null) ds.close();
    if (pg != null) pg.stop();
  }

  @Test
  void cachesSecondHitWithin24Hours() {
    AtomicInteger upstreamCalls = new AtomicInteger();
    Instant t0 = Instant.parse("2026-07-01T10:00:00Z");
    Clock clock = Clock.fixed(t0, ZoneOffset.UTC);
    ViesClient upstream =
        (country, vat) -> {
          upstreamCalls.incrementAndGet();
          return new ViesCheckResult(
              country, vat, true, Optional.of("Acme NV"), Optional.empty(), t0, "<raw/>");
        };
    var validator = new CachedViesValidator(upstream, ds, clock, Duration.ofHours(24));

    validator.validate(Country.of("BE"), "0123456789");
    validator.validate(Country.of("BE"), "0123456789");
    validator.validate(Country.of("BE"), "0123456789");

    assertThat(upstreamCalls.get()).isEqualTo(1);
  }

  @Test
  void refreshesAfter24Hours() {
    AtomicInteger upstreamCalls = new AtomicInteger();
    Instant base = Instant.parse("2026-07-02T10:00:00Z");
    java.util.concurrent.atomic.AtomicReference<Instant> nowRef =
        new java.util.concurrent.atomic.AtomicReference<>(base);
    Clock clock =
        new Clock() {
          @Override
          public java.time.ZoneId getZone() {
            return ZoneOffset.UTC;
          }

          @Override
          public Clock withZone(java.time.ZoneId zone) {
            return this;
          }

          @Override
          public Instant instant() {
            return nowRef.get();
          }
        };
    ViesClient upstream =
        (country, vat) -> {
          upstreamCalls.incrementAndGet();
          return new ViesCheckResult(
              country, vat, true, Optional.empty(), Optional.empty(), nowRef.get(), "<raw/>");
        };
    var validator = new CachedViesValidator(upstream, ds, clock, Duration.ofHours(24));

    validator.validate(Country.of("DE"), "123456789");
    nowRef.set(base.plus(Duration.ofHours(25)));
    validator.validate(Country.of("DE"), "123456789");

    assertThat(upstreamCalls.get()).isEqualTo(2);
  }

  @Test
  void normalizesWhitespaceAndCase() {
    AtomicInteger upstreamCalls = new AtomicInteger();
    Instant t0 = Instant.parse("2026-07-03T10:00:00Z");
    Clock clock = Clock.fixed(t0, ZoneOffset.UTC);
    ViesClient upstream =
        (country, vat) -> {
          upstreamCalls.incrementAndGet();
          return new ViesCheckResult(
              country, vat, true, Optional.empty(), Optional.empty(), t0, "<raw/>");
        };
    var validator = new CachedViesValidator(upstream, ds, clock, Duration.ofHours(24));

    validator.validate(Country.of("FR"), "fr 12345678901");
    validator.validate(Country.of("FR"), "FR12345678901");

    assertThat(upstreamCalls.get()).isEqualTo(1);
  }
}
