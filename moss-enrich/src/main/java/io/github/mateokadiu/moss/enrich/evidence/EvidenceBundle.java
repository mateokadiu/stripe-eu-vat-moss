package io.github.mateokadiu.moss.enrich.evidence;

import io.github.mateokadiu.moss.shared.Country;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * The collection of evidence pieces backing a single transaction's place-of-supply determination.
 *
 * <p>This object knows the EU rule: a B2C electronic supply must have two non-conflicting pieces of
 * evidence (one if the merchant is below the EUR 100k small-enterprise threshold).
 */
public record EvidenceBundle(List<EvidencePiece> pieces) {

  public EvidenceBundle {
    pieces = List.copyOf(pieces);
  }

  /** Returns true when all pieces agree on a single country. */
  public boolean allAgree() {
    return distinctCountries().size() <= 1;
  }

  /** Distinct countries observed across all pieces (empty country entries are skipped). */
  public Set<Country> distinctCountries() {
    Set<Country> seen = new HashSet<>();
    for (EvidencePiece p : pieces) {
      p.country().ifPresent(seen::add);
    }
    return seen;
  }

  /** Returns the agreed-upon country, if any. Empty when there is no consensus. */
  public Optional<Country> agreedCountry() {
    var d = distinctCountries();
    return d.size() == 1 ? d.stream().findFirst() : Optional.empty();
  }

  /** True if the bundle is sufficient given the small-enterprise flag. */
  public boolean isSufficient(boolean smallEnterprise) {
    int distinctTypes = (int) pieces.stream().map(EvidencePiece::type).distinct().count();
    int agreeingPieces =
        agreedCountry().isEmpty()
            ? 0
            : (int)
                pieces.stream()
                    .filter(p -> p.country().equals(agreedCountry()))
                    .map(EvidencePiece::type)
                    .distinct()
                    .count();
    int required = smallEnterprise ? 1 : 2;
    return agreeingPieces >= required && distinctTypes >= required;
  }

  /** Returns a per-type count of conflicting countries (only types that disagree). */
  public Map<EvidenceType, List<Country>> conflicts() {
    Map<EvidenceType, List<Country>> byType = new HashMap<>();
    for (EvidencePiece p : pieces) {
      p.country()
          .ifPresent(
              c -> byType.computeIfAbsent(p.type(), k -> new java.util.ArrayList<>()).add(c));
    }
    return byType;
  }
}
