package io.github.mateokadiu.moss.ingest.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.github.mateokadiu.moss.ingest.api.StripeTaxApiClient.StripeTaxTransaction;
import io.github.mateokadiu.moss.ingest.api.StripeTaxApiClient.StripeTaxTransactionsFetcher;
import io.github.mateokadiu.moss.ingest.cursor.IngestCursor;
import io.github.mateokadiu.moss.ingest.idempotency.SeenTransactions;
import io.github.mateokadiu.moss.ledger.Migrator;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class StripeTaxApiClientIT {

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
  void ingestsNewTransactionsAndAdvancesCursor() {
    StripeTaxTransactionsFetcher fetcher =
        (startingAfter, pageSize) ->
            List.of(
                new StripeTaxTransaction(
                    "txi_001", "EUR", 10000, Instant.parse("2026-07-01T00:00:00Z")),
                new StripeTaxTransaction(
                    "txi_002", "EUR", 20000, Instant.parse("2026-07-02T00:00:00Z")));

    var cursor = new IngestCursor(ds);
    var seen = new SeenTransactions(ds);
    var client = new StripeTaxApiClient(fetcher, cursor, seen);

    var ingested = client.drain(100);

    assertThat(ingested).containsExactly("txi_001", "txi_002");
    assertThat(cursor.position(StripeTaxApiClient.CURSOR_NAME)).contains("txi_002");
  }

  @Test
  void skipsAlreadySeenTransactionsOnReplay() {
    StripeTaxTransactionsFetcher fetcher =
        (startingAfter, pageSize) ->
            List.of(
                new StripeTaxTransaction(
                    "txi_010", "EUR", 100, Instant.parse("2026-07-03T00:00:00Z")),
                new StripeTaxTransaction(
                    "txi_011", "EUR", 200, Instant.parse("2026-07-04T00:00:00Z")));

    var cursor = new IngestCursor(ds);
    var seen = new SeenTransactions(ds);
    var client = new StripeTaxApiClient(fetcher, cursor, seen);

    var first = client.drain(50);
    var second = client.drain(50);

    assertThat(first).containsExactly("txi_010", "txi_011");
    assertThat(second).isEmpty();
  }
}
