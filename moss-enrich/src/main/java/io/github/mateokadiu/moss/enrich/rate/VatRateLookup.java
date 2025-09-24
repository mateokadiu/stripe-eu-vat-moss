package io.github.mateokadiu.moss.enrich.rate;

import io.github.mateokadiu.moss.shared.Country;
import java.time.LocalDate;
import java.util.Optional;

/** Lookups for VAT rates effective in a country on a specific date. */
public interface VatRateLookup {

  /** Returns the standard VAT rate in {@code country} effective on {@code date}, if any. */
  Optional<VatRate> standardRate(Country country, LocalDate date);

  /** Returns a specific {@code rateType} effective on {@code date}, if any. */
  Optional<VatRate> rate(Country country, RateType rateType, LocalDate date);
}
