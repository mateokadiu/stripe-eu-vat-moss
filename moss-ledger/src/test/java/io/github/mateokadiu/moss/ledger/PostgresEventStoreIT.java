package io.github.mateokadiu.moss.ledger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class PostgresEventStoreIT {

  private static PostgreSQLContainer<?> pg;
  private static HikariDataSource ds;
  private static PostgresEventStore store;

  @BeforeAll
  static void setup() {
    pg = new PostgreSQLContainer<>("postgres:16-alpine");
    pg.start();
    ds = newDataSource(pg);
    new Migrator(ds).migrate();
    store = new PostgresEventStore(ds);
  }

  @AfterAll
  static void teardown() {
    if (ds != null) ds.close();
    if (pg != null) pg.stop();
  }

  private static HikariDataSource newDataSource(PostgreSQLContainer<?> pg) {
    HikariConfig cfg = new HikariConfig();
    cfg.setJdbcUrl(pg.getJdbcUrl());
    cfg.setUsername(pg.getUsername());
    cfg.setPassword(pg.getPassword());
    cfg.setMaximumPoolSize(4);
    return new HikariDataSource(cfg);
  }

  @Test
  void appendsAndReplaysEvents() {
    UUID aggregate = UUID.randomUUID();
    Instant t0 = Instant.parse("2026-07-01T10:00:00Z");

    EventRecord e1 =
        EventRecord.newOpen(
            UUID.randomUUID(),
            aggregate,
            "Transaction",
            "TransactionRecorded",
            1,
            "{\"net\":12500}",
            "{}",
            t0,
            t0);
    EventRecord e2 =
        EventRecord.newOpen(
            UUID.randomUUID(),
            aggregate,
            "Transaction",
            "TransactionEnriched",
            2,
            "{\"vat\":2375}",
            "{}",
            t0.plusSeconds(60),
            t0.plusSeconds(60));

    store.appendAll(List.of(e1, e2));
    List<EventRecord> stream = store.loadStream(aggregate);

    assertThat(stream).hasSize(2);
    assertThat(stream.get(0).eventType()).isEqualTo("TransactionRecorded");
    assertThat(stream.get(0).payloadJson()).contains("\"net\"").contains("12500");
    assertThat(stream.get(1).version()).isEqualTo(2);
  }

  @Test
  void rejectsDuplicateVersion() {
    UUID aggregate = UUID.randomUUID();
    Instant t0 = Instant.parse("2026-07-02T10:00:00Z");

    EventRecord e1 =
        EventRecord.newOpen(
            UUID.randomUUID(), aggregate, "Transaction", "A", 1, "{}", "{}", t0, t0);
    EventRecord eDup =
        EventRecord.newOpen(
            UUID.randomUUID(), aggregate, "Transaction", "B", 1, "{}", "{}", t0, t0);

    store.append(e1);

    assertThatThrownBy(() -> store.append(eDup))
        .isInstanceOf(EventStore.ConcurrentModificationException.class);
  }

  @Test
  void asOfQueryHidesEventsRecordedLater() {
    UUID aggregate = UUID.randomUUID();
    Instant past = Instant.parse("2026-07-03T08:00:00Z");
    Instant present = Instant.parse("2026-07-03T12:00:00Z");

    EventRecord early =
        EventRecord.newOpen(
            UUID.randomUUID(), aggregate, "Transaction", "Recorded", 1, "{}", "{}", past, past);
    EventRecord late =
        EventRecord.newOpen(
            UUID.randomUUID(),
            aggregate,
            "Transaction",
            "Corrected",
            2,
            "{}",
            "{}",
            present,
            present);

    store.append(early);
    store.append(late);

    // as-of past+1s should see only the first event
    List<EventRecord> asOfPast = store.loadStreamAsOf(aggregate, past.plusSeconds(1));
    assertThat(asOfPast).hasSize(1).extracting(EventRecord::eventType).containsExactly("Recorded");

    // as-of now sees both
    List<EventRecord> asOfNow = store.loadStreamAsOf(aggregate, present.plusSeconds(1));
    assertThat(asOfNow).hasSize(2);
  }

  @Test
  void loadByTypeFiltersByValidTime() {
    UUID a1 = UUID.randomUUID();
    UUID a2 = UUID.randomUUID();
    Instant q3Start = Instant.parse("2026-07-01T00:00:00Z");
    Instant q3End = Instant.parse("2026-10-01T00:00:00Z");

    store.append(
        EventRecord.newOpen(
            UUID.randomUUID(),
            a1,
            "Transaction",
            "Recorded",
            1,
            "{}",
            "{}",
            Instant.parse("2026-08-15T00:00:00Z"),
            Instant.parse("2026-08-15T00:00:00Z")));
    store.append(
        EventRecord.newOpen(
            UUID.randomUUID(),
            a2,
            "Transaction",
            "Recorded",
            1,
            "{}",
            "{}",
            Instant.parse("2026-11-01T00:00:00Z"),
            Instant.parse("2026-11-01T00:00:00Z")));

    List<EventRecord> q3 = store.loadByTypeInRange("Transaction", q3Start, q3End);
    List<UUID> aggregates = q3.stream().map(EventRecord::aggregateId).toList();
    assertThat(aggregates).contains(a1).doesNotContain(a2);
  }

  static DataSource dataSource() {
    return ds;
  }
}
