package io.github.mateokadiu.moss.shared;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.IsoFields;

/**
 * A calendar-quarter period, used as the OSS reporting period (e.g. {@code 2026Q3}). Quarters are
 * 1-indexed: Q1 = Jan-Mar, Q2 = Apr-Jun, Q3 = Jul-Sep, Q4 = Oct-Dec.
 *
 * @param year the calendar year (4-digit)
 * @param quarter the quarter 1..4
 */
public record Period(int year, int quarter) implements Comparable<Period> {

  public Period {
    if (year < 1970 || year > 2999) {
      throw new IllegalArgumentException("year out of supported range [1970, 2999]: " + year);
    }
    if (quarter < 1 || quarter > 4) {
      throw new IllegalArgumentException("quarter must be 1..4: " + quarter);
    }
  }

  public static Period of(int year, int quarter) {
    return new Period(year, quarter);
  }

  /** Parses {@code yyyyQq} (e.g. {@code 2026Q3}). */
  public static Period parse(String code) {
    if (code == null || code.length() != 6 || code.charAt(4) != 'Q') {
      throw new IllegalArgumentException("invalid period code: " + code);
    }
    int y = Integer.parseInt(code.substring(0, 4));
    int q = Integer.parseInt(code.substring(5, 6));
    return new Period(y, q);
  }

  /** Returns the period a UTC instant falls into. */
  public static Period fromInstantUtc(Instant t) {
    LocalDate d = t.atZone(ZoneOffset.UTC).toLocalDate();
    return new Period(d.getYear(), d.get(IsoFields.QUARTER_OF_YEAR));
  }

  public static Period current(Clock clock) {
    return fromInstantUtc(clock.instant());
  }

  public Period previous() {
    return quarter == 1 ? new Period(year - 1, 4) : new Period(year, quarter - 1);
  }

  public Period next() {
    return quarter == 4 ? new Period(year + 1, 1) : new Period(year, quarter + 1);
  }

  public LocalDate firstDay() {
    Month month =
        switch (quarter) {
          case 1 -> Month.JANUARY;
          case 2 -> Month.APRIL;
          case 3 -> Month.JULY;
          case 4 -> Month.OCTOBER;
          default -> throw new IllegalStateException();
        };
    return LocalDate.of(year, month, 1);
  }

  public LocalDate lastDay() {
    LocalDate first = firstDay();
    return first.plusMonths(3).minusDays(1);
  }

  public Instant startUtc() {
    return LocalDateTime.of(firstDay(), LocalTime.MIN).toInstant(ZoneOffset.UTC);
  }

  public Instant endExclusiveUtc() {
    return LocalDateTime.of(firstDay().plusMonths(3), LocalTime.MIN).toInstant(ZoneOffset.UTC);
  }

  public boolean contains(Instant t) {
    return !t.isBefore(startUtc()) && t.isBefore(endExclusiveUtc());
  }

  public String code() {
    return year + "Q" + quarter;
  }

  /**
   * Filing deadline: last day of the month following the reporting period (Implementing Reg. (EU)
   * 2019/2026).
   */
  public LocalDate filingDeadline() {
    LocalDate dayAfterPeriod = firstDay().plusMonths(3);
    return dayAfterPeriod.withDayOfMonth(dayAfterPeriod.lengthOfMonth());
  }

  @Override
  public int compareTo(Period other) {
    int c = Integer.compare(this.year, other.year);
    return c != 0 ? c : Integer.compare(this.quarter, other.quarter);
  }

  /** Quarter that includes the given period for an OSS correction filing window (the next one). */
  public Period asReportingPeriodForCorrectionOf(ZonedDateTime now) {
    Period detected = fromInstantUtc(now.toInstant());
    return detected.compareTo(this) > 0 ? detected : this.next();
  }

  @Override
  public String toString() {
    return code();
  }
}
