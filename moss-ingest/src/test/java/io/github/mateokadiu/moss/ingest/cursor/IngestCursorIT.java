package io.github.mateokadiu.moss.ingest.cursor;

import static org.assertj.core.api.Assertions.assertThat;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.github.mateokadiu.moss.ingest.idempotency.SeenTransactions;
import io.github.mateokadiu.moss.ledger.Migrator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class IngestCursorIT {

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
  void cursorStartsEmpty() {
    var cursor = new IngestCursor(ds);
    assertThat(cursor.position("stripe.tax.transactions")).isEmpty();
  }

  @Test
  void advancesAndRereadsLatest() {
    var cursor = new IngestCursor(ds);
    cursor.advance("stripe.tax.transactions", "txi_100");
    cursor.advance("stripe.tax.transactions", "txi_200");

    assertThat(cursor.position("stripe.tax.transactions")).contains("txi_200");
  }

  @Test
  void claimIsExactlyOnce() {
    var seen = new SeenTransactions(ds);

    assertThat(seen.claim("tt_001")).isFalse();
    assertThat(seen.claim("tt_001")).isTrue();
    assertThat(seen.claim("tt_002")).isFalse();
    assertThat(seen.claim("tt_002")).isTrue();
  }
}
