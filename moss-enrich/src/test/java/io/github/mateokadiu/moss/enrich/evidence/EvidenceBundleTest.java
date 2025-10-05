package io.github.mateokadiu.moss.enrich.evidence;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.mateokadiu.moss.shared.Country;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EvidenceBundleTest {

  private final EvidenceCollector c = new EvidenceCollector();
  private final UUID tx = UUID.randomUUID();

  @Test
  void twoAgreeingPiecesAreSufficientForRegularMerchant() {
    var bundle =
        c.bundle(
            c.billingAddress(tx, Optional.of(Country.of("DE")), "{...}"),
            c.ipGeolocation(tx, Optional.of(Country.of("DE")), "203.0.113.1", "maxmind"));

    assertThat(bundle.allAgree()).isTrue();
    assertThat(bundle.isSufficient(false)).isTrue();
    assertThat(bundle.agreedCountry()).contains(Country.of("DE"));
  }

  @Test
  void onePieceIsInsufficientForRegularMerchant() {
    var bundle = c.bundle(c.billingAddress(tx, Optional.of(Country.of("DE")), "{...}"));

    assertThat(bundle.isSufficient(false)).isFalse();
  }

  @Test
  void onePieceIsSufficientForSmallEnterprise() {
    var bundle = c.bundle(c.billingAddress(tx, Optional.of(Country.of("DE")), "{...}"));

    assertThat(bundle.isSufficient(true)).isTrue();
  }

  @Test
  void conflictingPiecesAreInsufficient() {
    var bundle =
        c.bundle(
            c.billingAddress(tx, Optional.of(Country.of("DE")), "{...}"),
            c.ipGeolocation(tx, Optional.of(Country.of("FR")), "x", "maxmind"));

    assertThat(bundle.allAgree()).isFalse();
    assertThat(bundle.isSufficient(false)).isFalse();
    assertThat(bundle.agreedCountry()).isEmpty();
  }

  @Test
  void duplicatePieceTypesDoNotCountAsTwoPieces() {
    var bundle =
        c.bundle(
            c.billingAddress(tx, Optional.of(Country.of("BE")), "{a}"),
            c.billingAddress(tx, Optional.of(Country.of("BE")), "{b}"));

    assertThat(bundle.allAgree()).isTrue();
    // both pieces are BILLING_ADDRESS so distinct-type count == 1
    assertThat(bundle.isSufficient(false)).isFalse();
  }

  @Test
  void retentionUntilIsTenYearsAfterObservation() {
    var observed = java.time.Instant.parse("2026-07-15T12:00:00Z");
    var until = EvidenceCollector.retentionUntil(observed);

    assertThat(until).isEqualTo(java.time.LocalDate.of(2036, 7, 15));
  }
}
