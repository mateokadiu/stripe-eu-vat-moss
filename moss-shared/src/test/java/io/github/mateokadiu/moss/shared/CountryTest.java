package io.github.mateokadiu.moss.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class CountryTest {

  @Test
  void acceptsValidEuCode() {
    assertThat(Country.of("BE").isEuMemberState()).isTrue();
    assertThat(Country.of("DE").isEuMemberState()).isTrue();
  }

  @Test
  void rejectsLowercase() {
    assertThatThrownBy(() -> Country.of("be"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("uppercase");
  }

  @Test
  void rejectsUnknownCode() {
    assertThatThrownBy(() -> Country.of("XX")).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsWrongLength() {
    assertThatThrownBy(() -> Country.of("BEL")).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void recognisesNonEu() {
    // Switzerland is not EU
    assertThat(Country.of("CH").isEuMemberState()).isFalse();
  }

  @Test
  void hasExactly27EuMemberStates() {
    assertThat(Country.EU_MEMBER_STATES).hasSize(27);
  }
}
