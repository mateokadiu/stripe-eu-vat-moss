package io.github.mateokadiu.moss.ingest.idempotency;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.jooq.impl.DSL.table;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;
import javax.sql.DataSource;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

/**
 * Idempotency ledger keyed on Stripe's {@code tax_transaction_id}. CSV and webhook ingest both go
 * through this so a row is recorded exactly once even when an export overlaps a webhook.
 */
public final class SeenTransactions {

  private static final org.jooq.Table<?> T = table(name("seen_tax_transactions"));
  private static final org.jooq.Field<String> ID = field(name("tax_transaction_id"), String.class);

  private final DataSource dataSource;

  public SeenTransactions(DataSource dataSource) {
    this.dataSource = Objects.requireNonNull(dataSource);
  }

  /**
   * Returns true if {@code id} was previously claimed. Atomic: the first caller for an id sees
   * false and locks the row; subsequent callers see true.
   */
  public boolean claim(String id) {
    try (Connection conn = dataSource.getConnection()) {
      DSLContext ctx = DSL.using(conn, SQLDialect.POSTGRES);
      int inserted = ctx.insertInto(T).set(ID, id).onConflictDoNothing().execute();
      return inserted == 0;
    } catch (SQLException ex) {
      throw new IllegalStateException("could not claim tax_transaction_id", ex);
    }
  }
}
