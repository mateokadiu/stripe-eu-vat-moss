package io.github.mateokadiu.moss.file.generator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.mateokadiu.moss.file.generator.SafOssGenerator.CorrectionLine;
import io.github.mateokadiu.moss.file.generator.SafOssGenerator.SupplyLine;
import io.github.mateokadiu.moss.shared.Country;
import io.github.mateokadiu.moss.shared.Iso4217Currency;
import io.github.mateokadiu.moss.shared.Money;
import io.github.mateokadiu.moss.shared.Period;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class SafOssGeneratorTest {

  private final SafOssGenerator g = new SafOssGenerator("BE0123456789", Iso4217Currency.EUR);

  @Test
  void aggregatesByMemberStateAndRate() {
    var supplies =
        List.of(
            new SupplyLine(
                Country.of("DE"),
                new BigDecimal("19.0"),
                Money.of(50_000L, Iso4217Currency.EUR),
                Money.of(9_500L, Iso4217Currency.EUR)),
            new SupplyLine(
                Country.of("DE"),
                new BigDecimal("19.0"),
                Money.of(20_000L, Iso4217Currency.EUR),
                Money.of(3_800L, Iso4217Currency.EUR)),
            new SupplyLine(
                Country.of("FR"),
                new BigDecimal("20.0"),
                Money.of(10_000L, Iso4217Currency.EUR),
                Money.of(2_000L, Iso4217Currency.EUR)));

    var doc = g.build(Period.of(2026, 3), supplies, List.of());

    assertThat(doc.supplies()).hasSize(2);
    assertThat(doc.supplies().get(0).memberStateOfConsumption()).isEqualTo("DE");
    assertThat(doc.supplies().get(0).taxableAmount()).isEqualByComparingTo("700.00");
    assertThat(doc.supplies().get(0).vatAmount()).isEqualByComparingTo("133.00");
    assertThat(doc.supplies().get(1).memberStateOfConsumption()).isEqualTo("FR");
  }

  @Test
  void encodesRule_GuidelinesSec3_6_negativesNeverNetAcrossMs() {
    var corrections =
        List.of(
            new CorrectionLine(
                Period.of(2026, 1), Country.of("DE"), Money.of(-30_000L, Iso4217Currency.EUR)),
            new CorrectionLine(
                Period.of(2026, 1), Country.of("FR"), Money.of(20_000L, Iso4217Currency.EUR)));

    var doc = g.build(Period.of(2026, 3), List.of(), corrections);

    assertThat(doc.corrections()).hasSize(2);
    var de = doc.corrections().get(0);
    var fr = doc.corrections().get(1);
    assertThat(de.memberStateOfConsumption()).isEqualTo("DE");
    assertThat(de.deltaAmount()).isEqualByComparingTo("-300.00");
    assertThat(fr.memberStateOfConsumption()).isEqualTo("FR");
    assertThat(fr.deltaAmount()).isEqualByComparingTo("200.00");
  }

  @Test
  void refusesWrongCurrency() {
    var supplies =
        List.of(
            new SupplyLine(
                Country.of("DE"),
                new BigDecimal("19.0"),
                Money.of(50_000L, Iso4217Currency.USD),
                Money.of(9_500L, Iso4217Currency.USD)));

    assertThatThrownBy(() -> g.build(Period.of(2026, 3), supplies, List.of()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("currency mismatch");
  }

  @Test
  void groupsCorrectionsByOriginalPeriodAndMs() {
    var corrections =
        List.of(
            new CorrectionLine(
                Period.of(2026, 1), Country.of("DE"), Money.of(-10_000L, Iso4217Currency.EUR)),
            new CorrectionLine(
                Period.of(2026, 1), Country.of("DE"), Money.of(-5_000L, Iso4217Currency.EUR)),
            new CorrectionLine(
                Period.of(2026, 2), Country.of("DE"), Money.of(-7_000L, Iso4217Currency.EUR)));

    var doc = g.build(Period.of(2026, 3), List.of(), corrections);

    assertThat(doc.corrections()).hasSize(2);
    assertThat(doc.corrections().get(0).originalPeriod()).isEqualTo("2026Q1");
    assertThat(doc.corrections().get(0).deltaAmount()).isEqualByComparingTo("-150.00");
    assertThat(doc.corrections().get(1).originalPeriod()).isEqualTo("2026Q2");
  }
}
