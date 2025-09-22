package io.github.mateokadiu.moss.ledger;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;

/**
 * Replays an aggregate's event stream into a typed state object. Implementations may snapshot at
 * any version they like; replay is a fold over a stream.
 *
 * @param <S> the aggregate state type
 */
public final class BitemporalRepository<S> {

  private final EventStore store;
  private final BiFunction<S, EventRecord, S> apply;
  private final S empty;

  public BitemporalRepository(EventStore store, S emptyState, BiFunction<S, EventRecord, S> apply) {
    this.store = store;
    this.empty = emptyState;
    this.apply = apply;
  }

  /** Fold the current event stream into the latest state. */
  public Optional<S> load(UUID aggregateId) {
    var events = store.loadStream(aggregateId);
    if (events.isEmpty()) {
      return Optional.empty();
    }
    S s = empty;
    for (EventRecord e : events) {
      s = apply.apply(s, e);
    }
    return Optional.of(s);
  }

  /** "As-of" replay: produce state as it would have been at the given transaction time. */
  public Optional<S> loadAsOf(UUID aggregateId, Instant asOf) {
    var events = store.loadStreamAsOf(aggregateId, asOf);
    if (events.isEmpty()) {
      return Optional.empty();
    }
    S s = empty;
    for (EventRecord e : events) {
      s = apply.apply(s, e);
    }
    return Optional.of(s);
  }
}
