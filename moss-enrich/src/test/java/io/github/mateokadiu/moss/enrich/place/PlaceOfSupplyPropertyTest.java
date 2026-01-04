package io.github.mateokadiu.moss.enrich.place;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.mateokadiu.moss.enrich.evidence.EvidenceCollector;
import io.github.mateokadiu.moss.enrich.place.PlaceOfSupplyResolver.SupplyClassification;
import io.github.mateokadiu.moss.shared.Country;
import io.github.mateokadiu.moss.shared.Iso4217Currency;
import io.github.mateokadiu.moss.shared.Money;
import java.util.Optional;
import java.util.UUID;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.LongRange;

class PlaceOfSupplyPropertyTest {

  private final EvidenceCollector c = new EvidenceCollector();
  private final UUID tx = UUID.randomUUID();

  @Property
  void encodesRule_aboveTenKAlwaysCustomerCountryWhenAgree(
      @ForAll @LongRange(min = 10_000_00L, max = 1_000_000_00L) long minor) {
    var resolver = new PlaceOfSupplyResolver(Country.of("BE"), false);
    var bundle =
        c.bundle(
            c.billingAddress(tx, Optional.of(Country.of("DE")), "{}"),
            c.ipGeolocation(tx, Optional.of(Country.of("DE")), "x", "maxmind"));

    var decision = resolver.resolve(Money.of(minor, Iso4217Currency.EUR), bundle, false);

    assertThat(decision.classification()).isEqualTo(SupplyClassification.B2C_CROSS_BORDER);
    assertThat(decision.memberStateOfConsumption()).isEqualTo(Country.of("DE"));
  }

  @Property
  void encodesRule_belowTenKIsAlwaysDomestic(
      @ForAll @LongRange(min = 0L, max = 9_999_99L) long minor) {
    var resolver = new PlaceOfSupplyResolver(Country.of("BE"), false);
    var bundle =
        c.bundle(
            c.billingAddress(tx, Optional.of(Country.of("DE")), "{}"),
            c.ipGeolocation(tx, Optional.of(Country.of("DE")), "x", "maxmind"));

    var decision = resolver.resolve(Money.of(minor, Iso4217Currency.EUR), bundle, false);

    assertThat(decision.classification()).isEqualTo(SupplyClassification.B2C_DOMESTIC);
    assertThat(decision.memberStateOfConsumption()).isEqualTo(Country.of("BE"));
  }
}
