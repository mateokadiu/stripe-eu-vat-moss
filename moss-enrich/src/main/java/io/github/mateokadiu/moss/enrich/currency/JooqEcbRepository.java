package io.github.mateokadiu.moss.enrich.currency;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.jooq.impl.DSL.table;

import io.github.mateokadiu.moss.enrich.currency.EcbDailyFeedParser.EcbDailyRate;
import io.github.mateokadiu.moss.shared.Period;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.sql.DataSource;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

/** Persistence for ECB daily rates + quarter-pinned rates. */
public final class JooqEcbRepository {

  private static final org.jooq.Table<?> ECB = table(name("ecb_rates"));
  private static final org.jooq.Table<?> QUARTER = table(name("quarter_rates"));

  private static final org.jooq.Field<java.sql.Date> RATE_DATE =
      field(name("rate_date"), java.sql.Date.class);
  private static final org.jooq.Field<String> ECB_FROM = field(name("from_currency"), String.class);
  private static final org.jooq.Field<String> ECB_TO = field(name("to_currency"), String.class);
  private static final org.jooq.Field<BigDecimal> ECB_RATE = field(name("rate"), BigDecimal.class);

  private static final org.jooq.Field<String> Q_PERIOD = field(name("period_code"), String.class);
  private static final org.jooq.Field<String> Q_FROM = field(name("from_currency"), String.class);
  private static final org.jooq.Field<String> Q_TO = field(name("to_currency"), String.class);
  private static final org.jooq.Field<BigDecimal> Q_RATE = field(name("rate"), BigDecimal.class);

  private final DataSource dataSource;

  public JooqEcbRepository(DataSource dataSource) {
    this.dataSource = Objects.requireNonNull(dataSource);
  }

  /** Idempotently insert daily rates; existing (date, from, to) rows are left untouched. */
  public void upsertDaily(List<EcbDailyRate> rates) {
    if (rates.isEmpty()) {
      return;
    }
    try (Connection conn = dataSource.getConnection()) {
      conn.setAutoCommit(false);
      DSLContext ctx = DSL.using(conn, SQLDialect.POSTGRES);
      for (EcbDailyRate r : rates) {
        ctx.insertInto(ECB)
            .set(RATE_DATE, java.sql.Date.valueOf(r.date()))
            .set(ECB_FROM, r.fromCurrency())
            .set(ECB_TO, r.toCurrency())
            .set(ECB_RATE, r.rate())
            .onConflictDoNothing()
            .execute();
      }
      conn.commit();
    } catch (SQLException ex) {
      throw new IllegalStateException("could not upsert ECB rates", ex);
    }
  }

  public Optional<BigDecimal> findDaily(LocalDate date, String from, String to) {
    try (Connection conn = dataSource.getConnection()) {
      DSLContext ctx = DSL.using(conn, SQLDialect.POSTGRES);
      return Optional.ofNullable(
          ctx.select(ECB_RATE)
              .from(ECB)
              .where(RATE_DATE.eq(java.sql.Date.valueOf(date)))
              .and(ECB_FROM.eq(from))
              .and(ECB_TO.eq(to))
              .fetchOne(ECB_RATE));
    } catch (SQLException ex) {
      throw new IllegalStateException("could not read ECB rate", ex);
    }
  }

  /**
   * Pin a rate for a period. Idempotent — once pinned, future pins for the same triple are ignored.
   * Pinning a closed period freezes the rate so a future replay yields the same number.
   */
  public void pinForPeriod(Period period, String from, String to, BigDecimal rate) {
    try (Connection conn = dataSource.getConnection()) {
      DSLContext ctx = DSL.using(conn, SQLDialect.POSTGRES);
      ctx.insertInto(QUARTER)
          .set(Q_PERIOD, period.code())
          .set(Q_FROM, from)
          .set(Q_TO, to)
          .set(Q_RATE, rate)
          .onConflictDoNothing()
          .execute();
    } catch (SQLException ex) {
      throw new IllegalStateException("could not pin rate", ex);
    }
  }

  public Optional<BigDecimal> findPinned(Period period, String from, String to) {
    try (Connection conn = dataSource.getConnection()) {
      DSLContext ctx = DSL.using(conn, SQLDialect.POSTGRES);
      return Optional.ofNullable(
          ctx.select(Q_RATE)
              .from(QUARTER)
              .where(Q_PERIOD.eq(period.code()))
              .and(Q_FROM.eq(from))
              .and(Q_TO.eq(to))
              .fetchOne(Q_RATE));
    } catch (SQLException ex) {
      throw new IllegalStateException("could not read pinned rate", ex);
    }
  }
}
