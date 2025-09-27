package io.github.mateokadiu.moss.enrich.currency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.github.mateokadiu.moss.ledger.Migrator;
import io.github.mateokadiu.moss.shared.Iso4217Currency;
import io.github.mateokadiu.moss.shared.Money;
import io.github.mateokadiu.moss.shared.Period;
import java.math.BigDecimal;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class EcbCurrencyConverterIT {

  private static PostgreSQLContainer<?> pg;
  private static HikariDataSource ds;
  private static JooqEcbRepository repo;
  private static EcbCurrencyConverter converter;

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
    repo = new JooqEcbRepository(ds);
    converter = new EcbCurrencyConverter(repo);
  }

  @AfterAll
  static void teardown() {
    if (ds != null) ds.close();
    if (pg != null) pg.stop();
  }

  @Test
  void identityConversionLeavesAmountUntouched() {
    var amount = Money.of(10_000L, Iso4217Currency.EUR);
    assertThat(converter.convertForPeriod(amount, Iso4217Currency.EUR, Period.of(2026, 3)))
        .isEqualTo(amount);
  }

  @Test
  void refusesIfRateNotPinned() {
    var amount = Money.of(10_000L, Iso4217Currency.USD);

    assertThatThrownBy(
            () -> converter.convertForPeriod(amount, Iso4217Currency.EUR, Period.of(2026, 3)))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("no rate pinned");
  }

  @Test
  void usesPinnedRateForClosedPeriod() {
    Period q3 = Period.of(2026, 3);
    repo.pinForPeriod(q3, "USD", "EUR", new BigDecimal("0.9240"));

    var usd100 = Money.of(10_000L, Iso4217Currency.USD); // 100.00 USD
    var eur = converter.convertForPeriod(usd100, Iso4217Currency.EUR, q3);

    // 10000 * 0.9240 = 9240
    assertThat(eur.minorUnits()).isEqualTo(9240L);
    assertThat(eur.currency()).isEqualTo(Iso4217Currency.EUR);
  }

  @Test
  void pinIsIdempotent() {
    Period q4 = Period.of(2026, 4);
    repo.pinForPeriod(q4, "USD", "EUR", new BigDecimal("0.9000"));
    // second pin must NOT overwrite
    repo.pinForPeriod(q4, "USD", "EUR", new BigDecimal("0.5000"));

    var rate = repo.findPinned(q4, "USD", "EUR");
    assertThat(rate).isPresent();
    assertThat(rate.get()).isEqualByComparingTo(new BigDecimal("0.9000"));
  }
}
