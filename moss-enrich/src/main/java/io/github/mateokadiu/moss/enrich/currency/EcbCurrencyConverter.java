package io.github.mateokadiu.moss.enrich.currency;

import io.github.mateokadiu.moss.shared.Iso4217Currency;
import io.github.mateokadiu.moss.shared.Money;
import io.github.mateokadiu.moss.shared.Period;
import java.math.BigDecimal;

/**
 * Quarter-pinned ECB converter. Looks up the rate pinned for the given period; if none is present
 * the converter refuses to operate so we never silently use stale or live rates on closed periods.
 */
public final class EcbCurrencyConverter implements CurrencyConverter {

  private final JooqEcbRepository repo;

  public EcbCurrencyConverter(JooqEcbRepository repo) {
    this.repo = repo;
  }

  @Override
  public Money convertForPeriod(Money amount, Iso4217Currency targetCurrency, Period period) {
    if (amount.currency().equals(targetCurrency)) {
      return amount;
    }
    BigDecimal rate =
        repo.findPinned(period, amount.currency().code(), targetCurrency.code())
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "no rate pinned for "
                            + period.code()
                            + " "
                            + amount.currency().code()
                            + "->"
                            + targetCurrency.code()
                            + " — close the period first"));
    return CurrencyConverter.applyRateHalfUp(amount, rate, targetCurrency);
  }
}
