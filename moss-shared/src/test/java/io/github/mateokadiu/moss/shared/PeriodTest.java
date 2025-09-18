package io.github.mateokadiu.moss.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class PeriodTest {

  @Test
  void parsesCode() {
    assertThat(Period.parse("2026Q3")).isEqualTo(Period.of(2026, 3));
  }

  @Test
  void rendersCode() {
    assertThat(Period.of(2026, 3).code()).isEqualTo("2026Q3");
  }

  @Test
  void rejectsBadCode() {
    assertThatThrownBy(() -> Period.parse("2026-3"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("invalid period code");
  }

  @Test
  void rejectsQuarterOutOfRange() {
    assertThatThrownBy(() -> Period.of(2026, 5)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void nextWrapsAtYearBoundary() {
    assertThat(Period.of(2026, 4).next()).isEqualTo(Period.of(2027, 1));
  }

  @Test
  void previousWrapsAtYearBoundary() {
    assertThat(Period.of(2026, 1).previous()).isEqualTo(Period.of(2025, 4));
  }

  @Test
  void boundariesAreFirstAndLastDayOfQuarter() {
    Period q3 = Period.of(2026, 3);
    assertThat(q3.firstDay()).isEqualTo(LocalDate.of(2026, 7, 1));
    assertThat(q3.lastDay()).isEqualTo(LocalDate.of(2026, 9, 30));
  }

  @Test
  void filingDeadlineIsLastDayOfMonthAfterPeriod() {
    // OSS: Q3 due last day of October
    assertThat(Period.of(2026, 3).filingDeadline()).isEqualTo(LocalDate.of(2026, 10, 31));
    // Q4 due last day of January next year
    assertThat(Period.of(2026, 4).filingDeadline()).isEqualTo(LocalDate.of(2027, 1, 31));
  }

  @Test
  void containsInstants() {
    Period q3 = Period.of(2026, 3);
    assertThat(q3.contains(Instant.parse("2026-07-01T00:00:00Z"))).isTrue();
    assertThat(q3.contains(Instant.parse("2026-09-30T23:59:59Z"))).isTrue();
    assertThat(q3.contains(Instant.parse("2026-10-01T00:00:00Z"))).isFalse();
    assertThat(q3.contains(Instant.parse("2026-06-30T23:59:59Z"))).isFalse();
  }

  @Test
  void fromInstantUtcAttributesAcrossBoundary() {
    assertThat(Period.fromInstantUtc(Instant.parse("2026-06-30T23:59:59Z")))
        .isEqualTo(Period.of(2026, 2));
    assertThat(Period.fromInstantUtc(Instant.parse("2026-07-01T00:00:01Z")))
        .isEqualTo(Period.of(2026, 3));
  }

  @Test
  void compareNaturalOrder() {
    assertThat(Period.of(2026, 2)).isLessThan(Period.of(2026, 3));
    assertThat(Period.of(2026, 4)).isLessThan(Period.of(2027, 1));
  }
}
