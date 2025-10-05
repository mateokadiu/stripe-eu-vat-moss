package io.github.mateokadiu.moss.enrich.evidence;

import io.github.mateokadiu.moss.shared.Country;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * One piece of evidence-of-location attached to a transaction.
 *
 * <p>The five accepted types are listed in {@link EvidenceType}; per Council Implementing Reg.
 * 282/2011 Art. 24f a B2C electronic supply needs two non-conflicting pieces (one piece below the
 * EUR 100k small-enterprise threshold).
 */
public record EvidencePiece(
    UUID id,
    UUID transactionId,
    EvidenceType type,
    Optional<Country> country,
    String sourceProvider,
    String sourceRaw,
    String sourceHash,
    Instant observedAt) {

  public EvidencePiece {
    if (sourceProvider == null || sourceProvider.isBlank()) {
      throw new IllegalArgumentException("sourceProvider is required");
    }
  }
}
