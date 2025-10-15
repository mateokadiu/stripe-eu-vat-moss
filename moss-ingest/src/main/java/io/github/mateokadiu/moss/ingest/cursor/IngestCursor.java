package io.github.mateokadiu.moss.ingest.cursor;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.jooq.impl.DSL.table;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Optional;
import javax.sql.DataSource;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

/**
 * Persistent cursor for resumable ingest paths (CSV or API). Two methods: read where we left off,
 * advance to a new position. Cursor name uniquely identifies the ingest source.
 */
public final class IngestCursor {

  private static final org.jooq.Table<?> T = table(name("ingest_cursors"));
  private static final org.jooq.Field<String> NAME = field(name("cursor_name"), String.class);
  private static final org.jooq.Field<String> POS = field(name("position"), String.class);

  private final DataSource dataSource;

  public IngestCursor(DataSource dataSource) {
    this.dataSource = Objects.requireNonNull(dataSource);
  }

  public Optional<String> position(String cursorName) {
    try (Connection conn = dataSource.getConnection()) {
      DSLContext ctx = DSL.using(conn, SQLDialect.POSTGRES);
      return Optional.ofNullable(ctx.select(POS).from(T).where(NAME.eq(cursorName)).fetchOne(POS));
    } catch (SQLException ex) {
      throw new IllegalStateException("could not read cursor", ex);
    }
  }

  public void advance(String cursorName, String newPosition) {
    try (Connection conn = dataSource.getConnection()) {
      DSLContext ctx = DSL.using(conn, SQLDialect.POSTGRES);
      ctx.insertInto(T)
          .set(NAME, cursorName)
          .set(POS, newPosition)
          .onConflict(NAME)
          .doUpdate()
          .set(POS, newPosition)
          .execute();
    } catch (SQLException ex) {
      throw new IllegalStateException("could not advance cursor", ex);
    }
  }
}
