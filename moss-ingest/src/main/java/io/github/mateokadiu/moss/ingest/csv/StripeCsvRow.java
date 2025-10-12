package io.github.mateokadiu.moss.ingest.csv;

import io.github.mateokadiu.moss.shared.Country;
import io.github.mateokadiu.moss.shared.Iso4217Currency;
import io.github.mateokadiu.moss.shared.Money;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Domain projection of a single line from a Stripe Tax itemized CSV export.
 *
 * <p>The column set is what Stripe documents at https://docs.stripe.com/tax/reports. Unknown
 * columns are ignored; missing required columns throw at parse time.
 */
public record StripeCsvRow(
    String id,
    String taxTransactionId,
    StripeCsvType type,
    Iso4217Currency currency,
    Money subtotal,
    Money taxAmount,
    Instant transactionDateUtc,
    Country countryCode,
    Optional<String> stateCode,
    Optional<String> taxRate,
    Optional<String> taxName,
    Optional<String> customerTaxId,
    Optional<String> originAddress,
    Optional<String> destinationAddress) {

  public enum StripeCsvType {
    SALE,
    REFUND
  }

  public StripeCsvRow {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(taxTransactionId, "taxTransactionId");
    Objects.requireNonNull(type, "type");
    Objects.requireNonNull(currency, "currency");
    Objects.requireNonNull(subtotal, "subtotal");
    Objects.requireNonNull(taxAmount, "taxAmount");
    Objects.requireNonNull(transactionDateUtc, "transactionDateUtc");
    Objects.requireNonNull(countryCode, "countryCode");
  }
}
