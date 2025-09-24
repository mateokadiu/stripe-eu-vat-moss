package io.github.mateokadiu.moss.enrich.rate;

import io.github.mateokadiu.moss.shared.Country;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Optional;

/**
 * A VAT rate effective in a country for a specific rate type within a date range.
 *
 * @param country country code
 * @param rateType STANDARD, REDUCED, SUPER_REDUCED
 * @param basisPoints rate expressed in basis points (1900 = 19.00%)
 * @param effectiveFrom inclusive
 * @param effectiveTo inclusive end of validity, or empty for open-ended
 */
public record VatRate(
    Country country,
    RateType rateType,
    int basisPoints,
    LocalDate effectiveFrom,
    Optional<LocalDate> effectiveTo,
    String sourceNote) {

  /** Decimal form, e.g. 1900 basis points -> 0.1900 with scale 4. */
  public BigDecimal asDecimal() {
    return new BigDecimal(basisPoints).movePointLeft(4).setScale(4, RoundingMode.UNNECESSARY);
  }

  /** Human-readable percent string, e.g. "19.00%". */
  public String asPercent() {
    BigDecimal pct = new BigDecimal(basisPoints).movePointLeft(2);
    return pct.toPlainString() + "%";
  }

  public boolean appliesOn(LocalDate date) {
    if (date.isBefore(effectiveFrom)) {
      return false;
    }
    return effectiveTo.map(end -> !date.isAfter(end)).orElse(true);
  }
}
