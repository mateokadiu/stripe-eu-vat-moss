package io.github.mateokadiu.moss.enrich.place;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.mateokadiu.moss.enrich.evidence.EvidenceCollector;
import io.github.mateokadiu.moss.enrich.place.PlaceOfSupplyResolver.SupplyClassification;
import io.github.mateokadiu.moss.shared.Country;
import io.github.mateokadiu.moss.shared.Iso4217Currency;
import io.github.mateokadiu.moss.shared.Money;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PlaceOfSupplyResolverTest {

  private final EvidenceCollector c = new EvidenceCollector();
  private final UUID tx = UUID.randomUUID();

  @Test
  void encodesRule_Art59c_thresholdSwitchesPlaceOfSupplyToCustomerCountry() {
    var resolver = new PlaceOfSupplyResolver(Country.of("BE"), false);
    var bundle =
        c.bundle(
            c.billingAddress(tx, Optional.of(Country.of("DE")), "{}"),
            c.ipGeolocation(tx, Optional.of(Country.of("DE")), "x", "maxmind"));

    // below threshold -> supplier country
    var below =
        resolver.resolve(Money.of(500_000L, Iso4217Currency.EUR), bundle, false); // EUR 5,000
    assertThat(below.memberStateOfConsumption()).isEqualTo(Country.of("BE"));
    assertThat(below.classification()).isEqualTo(SupplyClassification.B2C_DOMESTIC);

    // at or above threshold -> customer country
    var above =
        resolver.resolve(
            Money.of(1_000_000L, Iso4217Currency.EUR), bundle, false); // EUR 10,000 exact
    assertThat(above.memberStateOfConsumption()).isEqualTo(Country.of("DE"));
    assertThat(above.classification()).isEqualTo(SupplyClassification.B2C_CROSS_BORDER);
  }

  @Test
  void encodesRule_Art196_b2bWithValidVatTriggersReverseCharge() {
    var resolver = new PlaceOfSupplyResolver(Country.of("BE"), false);
    var bundle = c.bundle(c.billingAddress(tx, Optional.of(Country.of("DE")), "{}"));

    var decision = resolver.resolve(Money.of(50_000_00L, Iso4217Currency.EUR), bundle, true);

    assertThat(decision.classification()).isEqualTo(SupplyClassification.B2B_REVERSE_CHARGE);
  }

  @Test
  void aboveThresholdWithConflictGoesToNeedsReview() {
    var resolver = new PlaceOfSupplyResolver(Country.of("BE"), false);
    var bundle =
        c.bundle(
            c.billingAddress(tx, Optional.of(Country.of("DE")), "{}"),
            c.ipGeolocation(tx, Optional.of(Country.of("FR")), "x", "maxmind"));

    var decision = resolver.resolve(Money.of(50_000_00L, Iso4217Currency.EUR), bundle, false);

    assertThat(decision.classification()).isEqualTo(SupplyClassification.NEEDS_REVIEW);
  }

  @Test
  void smallEnterpriseAcceptsOnePiece() {
    var resolver = new PlaceOfSupplyResolver(Country.of("BE"), true);
    var bundle = c.bundle(c.billingAddress(tx, Optional.of(Country.of("DE")), "{}"));

    var decision = resolver.resolve(Money.of(50_000_00L, Iso4217Currency.EUR), bundle, false);

    assertThat(decision.classification()).isEqualTo(SupplyClassification.B2C_CROSS_BORDER);
    assertThat(decision.memberStateOfConsumption()).isEqualTo(Country.of("DE"));
  }
}
