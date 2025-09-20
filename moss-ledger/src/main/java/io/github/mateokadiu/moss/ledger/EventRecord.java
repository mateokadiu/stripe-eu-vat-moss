package io.github.mateokadiu.moss.ledger;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * An immutable event row as persisted in the {@code events} table. Payload and metadata are
 * stringified JSON; the domain modules deserialize them into typed events.
 */
public record EventRecord(
    UUID id,
    UUID aggregateId,
    String aggregateType,
    String eventType,
    int version,
    String payloadJson,
    String metadataJson,
    Instant validTimeFrom,
    Optional<Instant> validTimeTo,
    Instant transactionTimeFrom,
    Optional<Instant> transactionTimeTo,
    Instant recordedAt) {

  /**
   * Construct a new event record (bitemporally open — no valid_time_to or transaction_time_to set).
   * The recorded_at timestamp is derived at insert by the DB.
   */
  public static EventRecord newOpen(
      UUID id,
      UUID aggregateId,
      String aggregateType,
      String eventType,
      int version,
      String payloadJson,
      String metadataJson,
      Instant validTimeFrom,
      Instant transactionTimeFrom) {
    return new EventRecord(
        id,
        aggregateId,
        aggregateType,
        eventType,
        version,
        payloadJson,
        metadataJson,
        validTimeFrom,
        Optional.empty(),
        transactionTimeFrom,
        Optional.empty(),
        transactionTimeFrom);
  }
}
