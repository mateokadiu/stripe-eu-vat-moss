package io.github.mateokadiu.moss.enrich.currency;

import io.github.mateokadiu.moss.shared.Iso4217Currency;
import io.github.mateokadiu.moss.shared.Money;
import io.github.mateokadiu.moss.shared.Period;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Converts amounts via the ECB reference rate on the last day of the tax period (per OSS rules,
 * Implementing Reg. (EU) 2019/2026).
 */
public interface CurrencyConverter {

  /**
   * Convert {@code amount} into {@code targetCurrency} using the ECB rate pinned for {@code
   * period}. Returns {@link Money} in target currency. Implementations must round half-up to the
   * target's minor unit.
   */
  Money convertForPeriod(Money amount, Iso4217Currency targetCurrency, Period period);

  /**
   * Helper for naive conversion outside a period — e.g. for previewing live amounts. Production
   * callers should always use {@link #convertForPeriod}.
   */
  static Money applyRateHalfUp(Money amount, BigDecimal rate, Iso4217Currency target) {
    BigDecimal converted = BigDecimal.valueOf(amount.minorUnits()).multiply(rate);
    long minor = converted.setScale(0, RoundingMode.HALF_UP).longValueExact();
    return new Money(minor, target);
  }
}
