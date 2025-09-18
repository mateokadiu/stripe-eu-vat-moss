package io.github.mateokadiu.moss.shared;

import java.util.Currency;
import java.util.Objects;

/**
 * An ISO-4217 currency identifier (e.g. {@code EUR}, {@code USD}). Validated against the JDK's
 * built-in currency registry at construction.
 */
public record Iso4217Currency(String code) {

  public Iso4217Currency {
    Objects.requireNonNull(code, "code");
    if (code.length() != 3) {
      throw new IllegalArgumentException("currency code must be 3 letters: " + code);
    }
    try {
      Currency.getInstance(code);
    } catch (IllegalArgumentException ex) {
      throw new IllegalArgumentException("unknown ISO-4217 currency: " + code, ex);
    }
  }

  public static final Iso4217Currency EUR = new Iso4217Currency("EUR");
  public static final Iso4217Currency USD = new Iso4217Currency("USD");
  public static final Iso4217Currency GBP = new Iso4217Currency("GBP");

  public static Iso4217Currency of(String code) {
    return new Iso4217Currency(code);
  }

  public int defaultFractionDigits() {
    return Currency.getInstance(code).getDefaultFractionDigits();
  }
}
