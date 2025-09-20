package io.github.mateokadiu.moss.ledger;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.inline;
import static org.jooq.impl.DSL.name;
import static org.jooq.impl.DSL.table;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.SQLDialect;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;

/** JOOQ-backed event store. All writes use prepared statements; no JPA, no Hibernate. */
public final class PostgresEventStore implements EventStore {

  private static final org.jooq.Table<Record> EVENTS = table(name("events"));
  private static final org.jooq.Field<UUID> ID = field(name("id"), UUID.class);
  private static final org.jooq.Field<UUID> AGGREGATE_ID = field(name("aggregate_id"), UUID.class);
  private static final org.jooq.Field<String> AGGREGATE_TYPE =
      field(name("aggregate_type"), String.class);
  private static final org.jooq.Field<String> EVENT_TYPE = field(name("event_type"), String.class);
  private static final org.jooq.Field<Integer> VERSION = field(name("version"), Integer.class);
  private static final org.jooq.Field<org.jooq.JSONB> PAYLOAD =
      field(name("payload"), org.jooq.JSONB.class);
  private static final org.jooq.Field<org.jooq.JSONB> METADATA =
      field(name("metadata"), org.jooq.JSONB.class);
  private static final org.jooq.Field<Timestamp> VALID_FROM =
      field(name("valid_time_from"), Timestamp.class);
  private static final org.jooq.Field<Timestamp> VALID_TO =
      field(name("valid_time_to"), Timestamp.class);
  private static final org.jooq.Field<Timestamp> TX_FROM =
      field(name("transaction_time_from"), Timestamp.class);
  private static final org.jooq.Field<Timestamp> TX_TO =
      field(name("transaction_time_to"), Timestamp.class);
  private static final org.jooq.Field<Timestamp> RECORDED_AT =
      field(name("recorded_at"), Timestamp.class);

  private final DataSource dataSource;

  public PostgresEventStore(DataSource dataSource) {
    this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
  }

  @Override
  public void append(EventRecord event) {
    appendAll(List.of(event));
  }

  @Override
  public void appendAll(List<EventRecord> events) {
    if (events.isEmpty()) {
      return;
    }
    try (Connection conn = dataSource.getConnection()) {
      conn.setAutoCommit(false);
      try {
        DSLContext ctx = DSL.using(conn, SQLDialect.POSTGRES);
        for (EventRecord e : events) {
          ctx.insertInto(EVENTS)
              .set(ID, e.id())
              .set(AGGREGATE_ID, e.aggregateId())
              .set(AGGREGATE_TYPE, e.aggregateType())
              .set(EVENT_TYPE, e.eventType())
              .set(VERSION, e.version())
              .set(
                  PAYLOAD, e.payloadJson() == null ? null : org.jooq.JSONB.valueOf(e.payloadJson()))
              .set(
                  METADATA,
                  e.metadataJson() == null ? null : org.jooq.JSONB.valueOf(e.metadataJson()))
              .set(VALID_FROM, Timestamp.from(e.validTimeFrom()))
              .set(VALID_TO, e.validTimeTo().map(Timestamp::from).orElse(null))
              .set(TX_FROM, Timestamp.from(e.transactionTimeFrom()))
              .set(TX_TO, e.transactionTimeTo().map(Timestamp::from).orElse(null))
              .execute();
        }
        conn.commit();
      } catch (DataAccessException ex) {
        conn.rollback();
        if (isUniqueViolation(ex)) {
          throw new ConcurrentModificationException(
              "duplicate (aggregate_id, version) — optimistic concurrency conflict");
        }
        throw ex;
      } catch (RuntimeException ex) {
        conn.rollback();
        throw ex;
      }
    } catch (SQLException ex) {
      throw new IllegalStateException("could not append events", ex);
    }
  }

  @Override
  public List<EventRecord> loadStream(UUID aggregateId) {
    try (Connection conn = dataSource.getConnection()) {
      DSLContext ctx = DSL.using(conn, SQLDialect.POSTGRES);
      return ctx.select(
              ID,
              AGGREGATE_ID,
              AGGREGATE_TYPE,
              EVENT_TYPE,
              VERSION,
              PAYLOAD,
              METADATA,
              VALID_FROM,
              VALID_TO,
              TX_FROM,
              TX_TO,
              RECORDED_AT)
          .from(EVENTS)
          .where(AGGREGATE_ID.eq(aggregateId))
          .orderBy(VERSION.asc())
          .fetch(this::toRecord);
    } catch (SQLException ex) {
      throw new IllegalStateException("could not load stream", ex);
    }
  }

  @Override
  public List<EventRecord> loadStreamAsOf(UUID aggregateId, Instant asOfTransactionTime) {
    try (Connection conn = dataSource.getConnection()) {
      DSLContext ctx = DSL.using(conn, SQLDialect.POSTGRES);
      Timestamp asOf = Timestamp.from(asOfTransactionTime);
      return ctx.select(
              ID,
              AGGREGATE_ID,
              AGGREGATE_TYPE,
              EVENT_TYPE,
              VERSION,
              PAYLOAD,
              METADATA,
              VALID_FROM,
              VALID_TO,
              TX_FROM,
              TX_TO,
              RECORDED_AT)
          .from(EVENTS)
          .where(AGGREGATE_ID.eq(aggregateId))
          .and(TX_FROM.le(asOf))
          .and(TX_TO.isNull().or(TX_TO.gt(asOf)))
          .orderBy(VERSION.asc())
          .fetch(this::toRecord);
    } catch (SQLException ex) {
      throw new IllegalStateException("could not load as-of", ex);
    }
  }

  @Override
  public List<EventRecord> loadByTypeInRange(
      String aggregateType, Instant from, Instant toExclusive) {
    try (Connection conn = dataSource.getConnection()) {
      DSLContext ctx = DSL.using(conn, SQLDialect.POSTGRES);
      Timestamp f = Timestamp.from(from);
      Timestamp t = Timestamp.from(toExclusive);
      return ctx.select(
              ID,
              AGGREGATE_ID,
              AGGREGATE_TYPE,
              EVENT_TYPE,
              VERSION,
              PAYLOAD,
              METADATA,
              VALID_FROM,
              VALID_TO,
              TX_FROM,
              TX_TO,
              RECORDED_AT)
          .from(EVENTS)
          .where(AGGREGATE_TYPE.eq(aggregateType))
          .and(VALID_FROM.ge(f))
          .and(VALID_FROM.lt(t))
          .orderBy(VALID_FROM.asc(), VERSION.asc())
          .fetch(this::toRecord);
    } catch (SQLException ex) {
      throw new IllegalStateException("could not load by type", ex);
    }
  }

  private EventRecord toRecord(org.jooq.Record r) {
    org.jooq.JSONB payload = r.get(PAYLOAD);
    org.jooq.JSONB metadata = r.get(METADATA);
    return new EventRecord(
        r.get(ID),
        r.get(AGGREGATE_ID),
        r.get(AGGREGATE_TYPE),
        r.get(EVENT_TYPE),
        r.get(VERSION),
        payload == null ? null : payload.data(),
        metadata == null ? null : metadata.data(),
        r.get(VALID_FROM).toInstant(),
        Optional.ofNullable(r.get(VALID_TO)).map(Timestamp::toInstant),
        r.get(TX_FROM).toInstant(),
        Optional.ofNullable(r.get(TX_TO)).map(Timestamp::toInstant),
        r.get(RECORDED_AT).toInstant());
  }

  private static boolean isUniqueViolation(DataAccessException ex) {
    Throwable cur = ex;
    while (cur != null) {
      if (cur instanceof SQLException sql) {
        return "23505".equals(sql.getSQLState());
      }
      cur = cur.getCause();
    }
    return false;
  }

  /** Convenience for tests/projection code wanting raw DSL access to the events table. */
  public static org.jooq.Condition eventTypeEquals(String eventType) {
    return EVENT_TYPE.eq(inline(eventType));
  }
}
