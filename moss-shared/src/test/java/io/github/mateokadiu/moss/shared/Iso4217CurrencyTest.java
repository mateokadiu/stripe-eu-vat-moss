package io.github.mateokadiu.moss.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class Iso4217CurrencyTest {

  @Test
  void acceptsEur() {
    assertThat(Iso4217Currency.of("EUR").code()).isEqualTo("EUR");
    assertThat(Iso4217Currency.of("EUR").defaultFractionDigits()).isEqualTo(2);
  }

  @Test
  void acceptsJpyWithZeroFractionDigits() {
    assertThat(Iso4217Currency.of("JPY").defaultFractionDigits()).isEqualTo(0);
  }

  @Test
  void rejectsUnknownCode() {
    assertThatThrownBy(() -> Iso4217Currency.of("XYZ"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsWrongLength() {
    assertThatThrownBy(() -> Iso4217Currency.of("EU")).isInstanceOf(IllegalArgumentException.class);
  }
}
