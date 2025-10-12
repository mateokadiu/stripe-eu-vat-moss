package io.github.mateokadiu.moss.ingest.csv;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.mateokadiu.moss.ingest.csv.StripeCsvRow.StripeCsvType;
import io.github.mateokadiu.moss.shared.Country;
import io.github.mateokadiu.moss.shared.Iso4217Currency;
import java.io.StringReader;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class StripeTaxCsvParserTest {

  private final StripeTaxCsvParser parser = new StripeTaxCsvParser();

  private static final String SAMPLE =
      """
      id,tax_transaction_id,type,currency,subtotal,tax_amount,transaction_date_utc,country_code,state_code,tax_rate,tax_name,customer_tax_id,origin_address,destination_address
      txi_001,tt_001,sale,EUR,100.00,19.00,2026-07-15T10:30:00Z,DE,,0.19,Mehrwertsteuer,,Berlin,Munich
      txi_002,tt_002,sale,EUR,200.00,42.00,2026-07-16T11:00:00Z,BE,,0.21,BTW,BE0123456789,Brussels,Antwerp
      txi_003,tt_003,refund,EUR,-50.00,-9.50,2026-07-17T12:00:00Z,DE,,0.19,Mehrwertsteuer,,Berlin,Hamburg
      """;

  @Test
  void parsesSaleRow() {
    var rows = parser.parseAll(new StringReader(SAMPLE));

    assertThat(rows).hasSize(3);
    var first = rows.get(0);
    assertThat(first.id()).isEqualTo("txi_001");
    assertThat(first.taxTransactionId()).isEqualTo("tt_001");
    assertThat(first.type()).isEqualTo(StripeCsvType.SALE);
    assertThat(first.currency()).isEqualTo(Iso4217Currency.EUR);
    assertThat(first.subtotal().minorUnits()).isEqualTo(10_000L);
    assertThat(first.taxAmount().minorUnits()).isEqualTo(1_900L);
    assertThat(first.transactionDateUtc()).isEqualTo(Instant.parse("2026-07-15T10:30:00Z"));
    assertThat(first.countryCode()).isEqualTo(Country.of("DE"));
  }

  @Test
  void parsesRefundWithNegativeAmounts() {
    var rows = parser.parseAll(new StringReader(SAMPLE));

    var refund = rows.get(2);
    assertThat(refund.type()).isEqualTo(StripeCsvType.REFUND);
    assertThat(refund.subtotal().minorUnits()).isEqualTo(-5_000L);
    assertThat(refund.taxAmount().minorUnits()).isEqualTo(-950L);
  }

  @Test
  void parsesB2BCustomerVatId() {
    var rows = parser.parseAll(new StringReader(SAMPLE));

    assertThat(rows.get(1).customerTaxId()).contains("BE0123456789");
  }

  @Test
  void rejectsMissingRequiredColumn() {
    String missingCountry =
        """
        id,tax_transaction_id,type,currency,subtotal,tax_amount,transaction_date_utc
        txi_004,tt_004,sale,EUR,100.00,19.00,2026-07-15T10:30:00Z
        """;

    assertThatThrownBy(() -> parser.parseAll(new StringReader(missingCountry)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("missing required column: country_code");
  }

  @Test
  void acceptsLegacyDateFormat() {
    String legacy =
        """
        id,tax_transaction_id,type,currency,subtotal,tax_amount,transaction_date_utc,country_code
        txi_010,tt_010,sale,EUR,100.00,19.00,2026-07-15 10:30:00,DE
        """;

    var rows = parser.parseAll(new StringReader(legacy));

    assertThat(rows.get(0).transactionDateUtc()).isEqualTo(Instant.parse("2026-07-15T10:30:00Z"));
  }
}
