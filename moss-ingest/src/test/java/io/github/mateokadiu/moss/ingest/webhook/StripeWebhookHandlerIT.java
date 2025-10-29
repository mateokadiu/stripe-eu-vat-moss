package io.github.mateokadiu.moss.ingest.webhook;

import static org.assertj.core.api.Assertions.assertThat;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.github.mateokadiu.moss.ledger.Migrator;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class StripeWebhookHandlerIT {

  private static final String SECRET = "whsec_testkey";

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
  void rejectsBadSignature() {
    Instant now = Instant.parse("2026-07-15T10:00:00Z");
    Clock clock = Clock.fixed(now, ZoneOffset.UTC);
    var handler = new StripeWebhookHandler(new StripeSignatureVerifier(SECRET), ds, clock);

    var outcome = handler.process("{}", "t=0,v1=0000000000", "evt_x", "tax.transaction.created");

    assertThat(outcome).isEqualTo(StripeWebhookHandler.Outcome.INVALID_SIGNATURE);
  }

  @Test
  void acceptsThenSkipsReplay() {
    Instant now = Instant.parse("2026-07-15T11:00:00Z");
    Clock clock = Clock.fixed(now, ZoneOffset.UTC);
    String payload = "{\"id\":\"evt_xyz\"}";
    long ts = now.getEpochSecond();
    String hmac = StripeSignatureVerifier.hmacSha256Hex(SECRET, ts + "." + payload);
    String header = "t=" + ts + ",v1=" + hmac;
    var handler = new StripeWebhookHandler(new StripeSignatureVerifier(SECRET), ds, clock);

    var first = handler.process(payload, header, "evt_xyz", "tax.transaction.created");
    var second = handler.process(payload, header, "evt_xyz", "tax.transaction.created");

    assertThat(first).isEqualTo(StripeWebhookHandler.Outcome.ACCEPTED);
    assertThat(second).isEqualTo(StripeWebhookHandler.Outcome.ALREADY_SEEN);
  }
}
