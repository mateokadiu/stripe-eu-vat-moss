package io.github.mateokadiu.moss.ledger;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * The append-only event log. Implementations must guarantee monotonic per-aggregate versioning via
 * the unique (aggregate_id, version) constraint enforced at the DB layer.
 */
public interface EventStore {

  /**
   * Append a single event. Throws {@link ConcurrentModificationException} if the (aggregate_id,
   * version) tuple already exists.
   */
  void append(EventRecord event);

  /** Append a batch atomically. Same uniqueness guarantee. */
  void appendAll(List<EventRecord> events);

  /** All events for an aggregate, ordered by version. */
  List<EventRecord> loadStream(UUID aggregateId);

  /**
   * Bitemporal "as-of" query: returns the event stream as it would have been visible at the given
   * transaction time. Use when reconstructing what a closed filing was based on.
   */
  List<EventRecord> loadStreamAsOf(UUID aggregateId, Instant asOfTransactionTime);

  /** All events of the given type recorded within [from, toExclusive). */
  List<EventRecord> loadByTypeInRange(String aggregateType, Instant from, Instant toExclusive);

  /** Exception thrown when an append would violate the per-aggregate version sequence. */
  class ConcurrentModificationException extends RuntimeException {
    public ConcurrentModificationException(String message) {
      super(message);
    }
  }
}
