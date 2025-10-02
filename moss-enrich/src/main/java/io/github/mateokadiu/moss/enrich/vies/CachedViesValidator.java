package io.github.mateokadiu.moss.enrich.vies;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.jooq.impl.DSL.table;

import io.github.mateokadiu.moss.shared.Country;
import io.github.mateokadiu.moss.shared.Ids;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

/**
 * VIES validator with a 24h cache (per VIES API guidance to avoid rate-limit ban). The cache is the
 * source of evidence for an audit: every successful check writes a row that's retained per the
 * 10-year OSS evidence rule (Council Directive 2006/112/EC Art. 369k).
 */
public final class CachedViesValidator {

  private static final org.jooq.Table<?> CACHE = table(name("vies_checks"));
  private static final org.jooq.Field<UUID> ID = field(name("id"), UUID.class);
  private static final org.jooq.Field<String> CC = field(name("country_code"), String.class);
  private static final org.jooq.Field<String> VAT = field(name("vat_number"), String.class);
  private static final org.jooq.Field<Boolean> VALID = field(name("valid"), Boolean.class);
  private static final org.jooq.Field<String> NAME = field(name("trader_name"), String.class);
  private static final org.jooq.Field<String> ADDR = field(name("trader_address"), String.class);
  private static final org.jooq.Field<String> RAW = field(name("raw_response"), String.class);
  private static final org.jooq.Field<Timestamp> CHECKED_AT =
      field(name("checked_at"), Timestamp.class);
  private static final org.jooq.Field<Timestamp> CACHE_UNTIL =
      field(name("cache_until"), Timestamp.class);

  private final ViesClient upstream;
  private final DataSource dataSource;
  private final Clock clock;
  private final Duration ttl;

  public CachedViesValidator(ViesClient upstream, DataSource dataSource) {
    this(upstream, dataSource, Clock.systemUTC(), Duration.ofHours(24));
  }

  public CachedViesValidator(
      ViesClient upstream, DataSource dataSource, Clock clock, Duration ttl) {
    this.upstream = upstream;
    this.dataSource = dataSource;
    this.clock = clock;
    this.ttl = ttl;
  }

  public ViesCheckResult validate(Country country, String vatNumber) {
    String normalized = vatNumber.replaceAll("\\s+", "").toUpperCase();
    return findFresh(country, normalized).orElseGet(() -> refresh(country, normalized));
  }

  Optional<ViesCheckResult> findFresh(Country country, String vatNumber) {
    Instant now = clock.instant();
    try (Connection conn = dataSource.getConnection()) {
      DSLContext ctx = DSL.using(conn, SQLDialect.POSTGRES);
      var rec =
          ctx.select(CC, VAT, VALID, NAME, ADDR, RAW, CHECKED_AT, CACHE_UNTIL)
              .from(CACHE)
              .where(CC.eq(country.code()))
              .and(VAT.eq(vatNumber))
              .and(CACHE_UNTIL.gt(Timestamp.from(now)))
              .orderBy(CHECKED_AT.desc())
              .limit(1)
              .fetchOne();
      if (rec == null) {
        return Optional.empty();
      }
      return Optional.of(
          new ViesCheckResult(
              Country.of(rec.get(CC)),
              rec.get(VAT),
              rec.get(VALID),
              Optional.ofNullable(rec.get(NAME)),
              Optional.ofNullable(rec.get(ADDR)),
              rec.get(CHECKED_AT).toInstant(),
              rec.get(RAW)));
    } catch (SQLException ex) {
      throw new IllegalStateException("could not read VIES cache", ex);
    }
  }

  private ViesCheckResult refresh(Country country, String vatNumber) {
    ViesCheckResult fresh = upstream.check(country, vatNumber);
    Instant checkedAt = fresh.checkedAt();
    Instant expires = checkedAt.plus(ttl);
    try (Connection conn = dataSource.getConnection()) {
      DSLContext ctx = DSL.using(conn, SQLDialect.POSTGRES);
      ctx.insertInto(CACHE)
          .set(ID, Ids.newV7())
          .set(CC, country.code())
          .set(VAT, vatNumber)
          .set(VALID, fresh.valid())
          .set(NAME, fresh.traderName().orElse(null))
          .set(ADDR, fresh.traderAddress().orElse(null))
          .set(RAW, fresh.rawResponse())
          .set(CHECKED_AT, Timestamp.from(checkedAt))
          .set(CACHE_UNTIL, Timestamp.from(expires))
          .execute();
    } catch (SQLException ex) {
      throw new IllegalStateException("could not write VIES cache", ex);
    }
    return fresh;
  }
}
