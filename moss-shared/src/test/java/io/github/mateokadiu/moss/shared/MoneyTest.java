package io.github.mateokadiu.moss.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.LongRange;
import org.junit.jupiter.api.Test;

class MoneyTest {

  private static final Iso4217Currency EUR = Iso4217Currency.EUR;
  private static final Iso4217Currency USD = Iso4217Currency.USD;

  @Test
  void addsMinorUnits() {
    Money a = Money.of(1250L, EUR);
    Money b = Money.of(50L, EUR);

    assertThat(a.plus(b)).isEqualTo(Money.of(1300L, EUR));
  }

  @Test
  void subtractsMinorUnits() {
    Money a = Money.of(1000L, EUR);
    Money b = Money.of(750L, EUR);

    assertThat(a.minus(b)).isEqualTo(Money.of(250L, EUR));
  }

  @Test
  void rejectsCurrencyMismatchOnAdd() {
    Money a = Money.of(100L, EUR);
    Money b = Money.of(100L, USD);

    assertThatThrownBy(() -> a.plus(b))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("currency mismatch");
  }

  @Test
  void appliesTaxRateHalfUp() {
    Money net = Money.of(10_000L, EUR); // 100.00 EUR
    BigDecimal vat = new BigDecimal("0.19");

    Money tax = net.applyRate(vat);

    assertThat(tax).isEqualTo(Money.of(1900L, EUR));
  }

  @Test
  void roundsHalfUpOnFractionalCents() {
    // 99 minor * 0.005 = 0.495 -> rounds half-up to 0
    // 100 minor * 0.005 = 0.5 -> rounds half-up to 1
    assertThat(Money.of(99L, EUR).applyRate(new BigDecimal("0.005"))).isEqualTo(Money.of(0L, EUR));
    assertThat(Money.of(100L, EUR).applyRate(new BigDecimal("0.005"))).isEqualTo(Money.of(1L, EUR));
  }

  @Test
  void majorRoundtrip() {
    Money m = Money.ofMajor(new BigDecimal("12.50"), EUR);
    assertThat(m.minorUnits()).isEqualTo(1250L);
    assertThat(m.toMajor()).isEqualByComparingTo(new BigDecimal("12.50"));
  }

  @Property
  void plusIsCommutative(
      @ForAll @LongRange(min = -1_000_000L, max = 1_000_000L) long a,
      @ForAll @LongRange(min = -1_000_000L, max = 1_000_000L) long b) {
    Money m1 = Money.of(a, EUR);
    Money m2 = Money.of(b, EUR);

    assertThat(m1.plus(m2)).isEqualTo(m2.plus(m1));
  }

  @Property
  void plusAndMinusAreInverses(
      @ForAll @LongRange(min = -1_000_000L, max = 1_000_000L) long a,
      @ForAll @LongRange(min = -1_000_000L, max = 1_000_000L) long b) {
    Money m1 = Money.of(a, EUR);
    Money m2 = Money.of(b, EUR);

    assertThat(m1.plus(m2).minus(m2)).isEqualTo(m1);
  }

  @Property
  void negateNegate(@ForAll @LongRange(min = -1_000_000L, max = 1_000_000L) long a) {
    Money m = Money.of(a, EUR);
    assertThat(m.negate().negate()).isEqualTo(m);
  }
}
