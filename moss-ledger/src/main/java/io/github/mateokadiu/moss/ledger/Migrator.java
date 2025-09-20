package io.github.mateokadiu.moss.ledger;

import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import liquibase.Liquibase;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;

/** Runs Liquibase migrations against a data source. Pure JDBC entry point — no Spring required. */
public final class Migrator {

  private final DataSource dataSource;
  private final String changelog;

  public Migrator(DataSource dataSource) {
    this(dataSource, "db/changelog/db.changelog-master.yaml");
  }

  public Migrator(DataSource dataSource, String changelog) {
    this.dataSource = dataSource;
    this.changelog = changelog;
  }

  public void migrate() {
    try (Connection conn = dataSource.getConnection()) {
      var db =
          DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(conn));
      try (Liquibase liquibase = new Liquibase(changelog, new ClassLoaderResourceAccessor(), db)) {
        liquibase.update("");
      }
    } catch (SQLException | LiquibaseException ex) {
      throw new IllegalStateException("liquibase migrate failed", ex);
    }
  }
}
