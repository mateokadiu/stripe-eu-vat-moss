package io.github.mateokadiu.moss.shared;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

/**
 * A money value stored as a signed minor-unit amount (cents for EUR/USD, etc.) plus an ISO-4217
 * currency. Operating on Money requires both sides to share the same currency.
 *
 * <p>Storing minor units as a {@code long} avoids the BigDecimal allocation cost on every read.
 * Conversion to/from major-unit decimal happens only at the API edge.
 *
 * @param minorUnits signed amount in the currency's minor unit (e.g. cents)
 * @param currency the ISO-4217 currency
 */
public record Money(long minorUnits, Iso4217Currency currency) {

  public Money {
    Objects.requireNonNull(currency, "currency");
  }

  public static Money of(long minorUnits, Iso4217Currency currency) {
    return new Money(minorUnits, currency);
  }

  public static Money zero(Iso4217Currency currency) {
    return new Money(0L, currency);
  }

  /**
   * Builds a Money from a major-unit decimal (e.g. {@code 12.50}) and a currency. Half-up rounded
   * to the currency's fraction digits.
   */
  public static Money ofMajor(BigDecimal major, Iso4217Currency currency) {
    Objects.requireNonNull(major, "major");
    Objects.requireNonNull(currency, "currency");
    int fractionDigits = Currency.getInstance(currency.code()).getDefaultFractionDigits();
    BigDecimal scaled = major.setScale(fractionDigits, RoundingMode.HALF_UP);
    return new Money(scaled.movePointRight(fractionDigits).longValueExact(), currency);
  }

  /** Returns the major-unit decimal representation (e.g. 1250 cents EUR -> 12.50). */
  public BigDecimal toMajor() {
    int fractionDigits = Currency.getInstance(currency.code()).getDefaultFractionDigits();
    return BigDecimal.valueOf(minorUnits).movePointLeft(fractionDigits);
  }

  public Money plus(Money other) {
    requireSameCurrency(other);
    return new Money(Math.addExact(this.minorUnits, other.minorUnits), currency);
  }

  public Money minus(Money other) {
    requireSameCurrency(other);
    return new Money(Math.subtractExact(this.minorUnits, other.minorUnits), currency);
  }

  public Money negate() {
    return new Money(Math.negateExact(this.minorUnits), currency);
  }

  public Money abs() {
    return minorUnits < 0 ? negate() : this;
  }

  public boolean isNegative() {
    return minorUnits < 0;
  }

  public boolean isPositive() {
    return minorUnits > 0;
  }

  public boolean isZero() {
    return minorUnits == 0L;
  }

  /**
   * Multiplies this amount by a tax rate expressed as a decimal (e.g. 0.19 for 19% VAT) using
   * half-up rounding to the currency's minor unit.
   */
  public Money applyRate(BigDecimal rate) {
    Objects.requireNonNull(rate, "rate");
    BigDecimal product = BigDecimal.valueOf(minorUnits).multiply(rate);
    return new Money(product.setScale(0, RoundingMode.HALF_UP).longValueExact(), currency);
  }

  private void requireSameCurrency(Money other) {
    if (!Objects.equals(this.currency, other.currency)) {
      throw new IllegalArgumentException(
          "currency mismatch: " + this.currency + " vs " + other.currency);
    }
  }

  @Override
  public String toString() {
    return toMajor().toPlainString() + " " + currency.code();
  }
}
