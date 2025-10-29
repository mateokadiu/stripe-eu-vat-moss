package io.github.mateokadiu.moss.ingest.webhook;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.jooq.impl.DSL.table;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.util.Objects;
import javax.sql.DataSource;
import org.jooq.DSLContext;
import org.jooq.JSONB;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

/**
 * Handles inbound Stripe webhook calls.
 *
 * <p>Two responsibilities: (1) verify the signature, (2) idempotently log the event payload keyed
 * on Stripe's {@code event.id}. The actual fact processing is done by the regular ingest pipeline
 * which polls the events table — webhooks just notify, they never carry the source of truth.
 */
public final class StripeWebhookHandler {

  private static final org.jooq.Table<?> T = table(name("webhook_events"));
  private static final org.jooq.Field<String> EVENT_ID = field(name("event_id"), String.class);
  private static final org.jooq.Field<String> EVENT_TYPE = field(name("event_type"), String.class);
  private static final org.jooq.Field<JSONB> PAYLOAD = field(name("payload"), JSONB.class);

  private final StripeSignatureVerifier verifier;
  private final DataSource dataSource;
  private final Clock clock;

  public StripeWebhookHandler(
      StripeSignatureVerifier verifier, DataSource dataSource, Clock clock) {
    this.verifier = Objects.requireNonNull(verifier);
    this.dataSource = Objects.requireNonNull(dataSource);
    this.clock = Objects.requireNonNull(clock);
  }

  /**
   * Process a webhook request body + signature header. Returns the outcome enum; the HTTP layer
   * maps that to a status code.
   */
  public Outcome process(String rawBody, String signatureHeader, String eventId, String eventType) {
    if (!verifier.verify(rawBody, signatureHeader, clock.instant())) {
      return Outcome.INVALID_SIGNATURE;
    }
    try (Connection conn = dataSource.getConnection()) {
      DSLContext ctx = DSL.using(conn, SQLDialect.POSTGRES);
      int rows =
          ctx.insertInto(T)
              .set(EVENT_ID, eventId)
              .set(EVENT_TYPE, eventType)
              .set(PAYLOAD, JSONB.valueOf(rawBody))
              .onConflictDoNothing()
              .execute();
      return rows == 0 ? Outcome.ALREADY_SEEN : Outcome.ACCEPTED;
    } catch (SQLException ex) {
      throw new IllegalStateException("could not record webhook event", ex);
    }
  }

  /** Webhook processing outcome. */
  public enum Outcome {
    ACCEPTED,
    ALREADY_SEEN,
    INVALID_SIGNATURE
  }
}
