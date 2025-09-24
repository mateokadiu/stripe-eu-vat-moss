package io.github.mateokadiu.moss.enrich.rate;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.jooq.impl.DSL.table;

import io.github.mateokadiu.moss.shared.Country;
import java.sql.Connection;
import java.sql.Date;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import javax.sql.DataSource;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

/**
 * Reads the {@code vat_rates} liquibase-seeded table. Picks the row whose [effective_from,
 * effective_to] window contains the given date.
 */
public final class JooqVatRateLookup implements VatRateLookup {

  private static final org.jooq.Table<?> VAT_RATES = table(name("vat_rates"));
  private static final org.jooq.Field<String> COUNTRY = field(name("country_code"), String.class);
  private static final org.jooq.Field<String> RATE_TYPE = field(name("rate_type"), String.class);
  private static final org.jooq.Field<Integer> RATE_BP =
      field(name("rate_basis_points"), Integer.class);
  private static final org.jooq.Field<Date> FROM = field(name("effective_from"), Date.class);
  private static final org.jooq.Field<Date> TO = field(name("effective_to"), Date.class);
  private static final org.jooq.Field<String> NOTE = field(name("source_note"), String.class);

  private final DataSource dataSource;

  public JooqVatRateLookup(DataSource dataSource) {
    this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
  }

  @Override
  public Optional<VatRate> standardRate(Country country, LocalDate date) {
    return rate(country, RateType.STANDARD, date);
  }

  @Override
  public Optional<VatRate> rate(Country country, RateType rateType, LocalDate date) {
    try (Connection conn = dataSource.getConnection()) {
      DSLContext ctx = DSL.using(conn, SQLDialect.POSTGRES);
      var rec =
          ctx.select(COUNTRY, RATE_TYPE, RATE_BP, FROM, TO, NOTE)
              .from(VAT_RATES)
              .where(COUNTRY.eq(country.code()))
              .and(RATE_TYPE.eq(rateType.name()))
              .and(FROM.le(Date.valueOf(date)))
              .and(TO.isNull().or(TO.ge(Date.valueOf(date))))
              .orderBy(FROM.desc())
              .limit(1)
              .fetchOne();
      if (rec == null) {
        return Optional.empty();
      }
      return Optional.of(
          new VatRate(
              Country.of(rec.get(COUNTRY)),
              RateType.valueOf(rec.get(RATE_TYPE)),
              rec.get(RATE_BP),
              rec.get(FROM).toLocalDate(),
              Optional.ofNullable(rec.get(TO)).map(Date::toLocalDate),
              rec.get(NOTE)));
    } catch (SQLException ex) {
      throw new IllegalStateException("could not query vat_rates", ex);
    }
  }
}
