package io.github.mateokadiu.moss.enrich.rate;

import static org.assertj.core.api.Assertions.assertThat;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.github.mateokadiu.moss.ledger.Migrator;
import io.github.mateokadiu.moss.shared.Country;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class JooqVatRateLookupIT {

  private static PostgreSQLContainer<?> pg;
  private static HikariDataSource ds;
  private static JooqVatRateLookup lookup;

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
    lookup = new JooqVatRateLookup(ds);
  }

  @AfterAll
  static void teardown() {
    if (ds != null) ds.close();
    if (pg != null) pg.stop();
  }

  @Test
  void looksUpGermanStandardRate() {
    var rate = lookup.standardRate(Country.of("DE"), LocalDate.of(2026, 7, 15));

    assertThat(rate).isPresent();
    assertThat(rate.get().basisPoints()).isEqualTo(1900);
    assertThat(rate.get().asDecimal()).isEqualByComparingTo(new BigDecimal("0.1900"));
    assertThat(rate.get().asPercent()).isEqualTo("19.00%");
  }

  @Test
  void hungaryIsHighest() {
    var rate = lookup.standardRate(Country.of("HU"), LocalDate.of(2026, 7, 15));

    assertThat(rate).isPresent();
    assertThat(rate.get().basisPoints()).isEqualTo(2700);
  }

  @Test
  void luxembourgIsLowest() {
    var rate = lookup.standardRate(Country.of("LU"), LocalDate.of(2026, 7, 15));

    assertThat(rate).isPresent();
    assertThat(rate.get().basisPoints()).isEqualTo(1700);
  }

  @Test
  void finlandReducedRateDropsIn2026() {
    var before = lookup.rate(Country.of("FI"), RateType.REDUCED, LocalDate.of(2025, 12, 31));
    var after = lookup.rate(Country.of("FI"), RateType.REDUCED, LocalDate.of(2026, 1, 1));

    assertThat(before).isEmpty(); // we did not seed the pre-2026 reduced rate row
    assertThat(after).isPresent();
    assertThat(after.get().basisPoints()).isEqualTo(1350);
  }

  @Test
  void lithuaniaReducedRateRisesIn2026() {
    var after = lookup.rate(Country.of("LT"), RateType.REDUCED, LocalDate.of(2026, 1, 1));

    assertThat(after).isPresent();
    assertThat(after.get().basisPoints()).isEqualTo(1200);
  }

  @Test
  void unknownCountryReturnsEmpty() {
    // Country.of validates ISO codes; using a non-EU one
    var rate = lookup.standardRate(Country.of("CH"), LocalDate.of(2026, 7, 15));

    assertThat(rate).isEmpty();
  }
}
