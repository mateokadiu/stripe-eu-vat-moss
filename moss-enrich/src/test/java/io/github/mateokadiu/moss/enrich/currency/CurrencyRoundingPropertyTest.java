package io.github.mateokadiu.moss.enrich.currency;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.mateokadiu.moss.shared.Iso4217Currency;
import io.github.mateokadiu.moss.shared.Money;
import java.math.BigDecimal;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.LongRange;

class CurrencyRoundingPropertyTest {

  @Property
  void rateApplicationStaysBounded(@ForAll @LongRange(min = 0L, max = 1_000_000_00L) long minor) {
    var amount = Money.of(minor, Iso4217Currency.USD);
    var rate = new BigDecimal("0.5000");

    var converted = CurrencyConverter.applyRateHalfUp(amount, rate, Iso4217Currency.EUR);

    // Conversion at 0.5 must be at most half + 1 cent (rounding up) and at least half - 1 cent.
    assertThat(converted.minorUnits()).isBetween(minor / 2 - 1, minor / 2 + 1);
    assertThat(converted.currency()).isEqualTo(Iso4217Currency.EUR);
  }

  @Property
  void zeroAmountStaysZero(@ForAll @LongRange(min = 0L, max = 100_0000L) long rateLongFixedPoint) {
    BigDecimal rate = new BigDecimal(rateLongFixedPoint).movePointLeft(4);
    var amount = Money.zero(Iso4217Currency.EUR);
    var converted = CurrencyConverter.applyRateHalfUp(amount, rate, Iso4217Currency.USD);

    assertThat(converted.minorUnits()).isZero();
  }
}
