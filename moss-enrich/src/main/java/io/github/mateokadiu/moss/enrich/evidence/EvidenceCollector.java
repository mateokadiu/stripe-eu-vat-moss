package io.github.mateokadiu.moss.enrich.evidence;

import io.github.mateokadiu.moss.shared.Country;
import io.github.mateokadiu.moss.shared.Ids;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Builds an {@link EvidenceBundle} for a transaction. The collector is intentionally pure (no I/O)
 * — callers feed it raw Stripe / MaxMind / customer-form data and it bundles + hashes.
 *
 * <p>Rule encoded here: EU Council Implementing Reg. 282/2011 Art. 24f — two non-conflicting
 * evidences for B2C electronic services; one is enough under EUR 100k turnover.
 */
public final class EvidenceCollector {

  private final Clock clock;

  public EvidenceCollector() {
    this(Clock.systemUTC());
  }

  public EvidenceCollector(Clock clock) {
    this.clock = clock;
  }

  public EvidencePiece billingAddress(UUID transactionId, Optional<Country> country, String raw) {
    return piece(transactionId, EvidenceType.BILLING_ADDRESS, country, "stripe.billing", raw);
  }

  public EvidencePiece ipGeolocation(
      UUID transactionId, Optional<Country> country, String ip, String provider) {
    String raw = "ip=" + (ip == null ? "" : ip);
    return piece(transactionId, EvidenceType.IP_GEOLOCATION, country, provider, raw);
  }

  public EvidencePiece bankLocation(UUID transactionId, Optional<Country> country, String raw) {
    return piece(transactionId, EvidenceType.BANK_LOCATION, country, "stripe.card", raw);
  }

  public EvidencePiece mccPhone(UUID transactionId, Optional<Country> country, String raw) {
    return piece(transactionId, EvidenceType.MCC_PHONE, country, "telco.sim", raw);
  }

  public EvidencePiece customerDeclared(UUID transactionId, Optional<Country> country, String raw) {
    return piece(transactionId, EvidenceType.CUSTOMER_DECLARED, country, "checkout.declared", raw);
  }

  /** Build a bundle from a non-null list of pieces. */
  public EvidenceBundle bundle(EvidencePiece... pieces) {
    List<EvidencePiece> nonNull = new ArrayList<>();
    for (EvidencePiece p : pieces) {
      if (p != null) {
        nonNull.add(p);
      }
    }
    return new EvidenceBundle(nonNull);
  }

  /** Returns the retention deadline 10y after the observed time, per Directive 2006/112/EC. */
  public static LocalDate retentionUntil(Instant observedAt) {
    return observedAt.atZone(ZoneOffset.UTC).toLocalDate().plusYears(10);
  }

  private EvidencePiece piece(
      UUID transactionId,
      EvidenceType type,
      Optional<Country> country,
      String provider,
      String raw) {
    return new EvidencePiece(
        Ids.newV7(), transactionId, type, country, provider, raw, sha256(raw), clock.instant());
  }

  static String sha256(String s) {
    if (s == null) {
      return "";
    }
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      byte[] hash = md.digest(s.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash);
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException(ex);
    }
  }
}
