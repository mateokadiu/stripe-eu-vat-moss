package io.github.mateokadiu.moss.ingest.api;

import io.github.mateokadiu.moss.ingest.cursor.IngestCursor;
import io.github.mateokadiu.moss.ingest.idempotency.SeenTransactions;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Thin orchestrator around Stripe's Standalone Tax API. Pulls {@code GET /v1/tax/transactions}
 * pages, keyed by a resumable cursor and an idempotency table. The actual HTTP call is delegated to
 * {@link StripeTaxTransactionsFetcher} which the production wiring implements via the Stripe Java
 * SDK; tests inject a fake.
 */
public final class StripeTaxApiClient {

  public static final String CURSOR_NAME = "stripe.tax.transactions";

  private static final Logger LOG = LoggerFactory.getLogger(StripeTaxApiClient.class);

  private final StripeTaxTransactionsFetcher fetcher;
  private final IngestCursor cursor;
  private final SeenTransactions seen;

  public StripeTaxApiClient(
      StripeTaxTransactionsFetcher fetcher, IngestCursor cursor, SeenTransactions seen) {
    this.fetcher = Objects.requireNonNull(fetcher);
    this.cursor = Objects.requireNonNull(cursor);
    this.seen = Objects.requireNonNull(seen);
  }

  /**
   * Drain transactions newer than the last cursor position. The cursor stores the latest seen
   * {@code id}; the fetcher must return transactions in ascending creation order (default for
   * Stripe with {@code starting_after}).
   *
   * @return the list of fresh transaction ids ingested
   */
  public List<String> drain(int pageSize) {
    Optional<String> startingAfter = cursor.position(CURSOR_NAME);
    List<StripeTaxTransaction> page = fetcher.fetch(startingAfter, pageSize);
    java.util.List<String> ingested = new java.util.ArrayList<>();
    String latest = startingAfter.orElse(null);
    for (StripeTaxTransaction t : page) {
      if (seen.claim(t.id())) {
        LOG.debug("skipping duplicate tax_transaction {}", t.id());
        continue;
      }
      ingested.add(t.id());
      latest = t.id();
    }
    if (latest != null && !latest.equals(startingAfter.orElse(null))) {
      cursor.advance(CURSOR_NAME, latest);
    }
    return ingested;
  }

  /** Minimal view over a Stripe tax transaction. The full payload is held by the fetcher impl. */
  public record StripeTaxTransaction(String id, String currency, long amount, Instant created) {}

  /** Pluggable transport layer. The Stripe SDK impl lives in {@code wiring}. */
  public interface StripeTaxTransactionsFetcher {
    List<StripeTaxTransaction> fetch(Optional<String> startingAfter, int pageSize);
  }
}
