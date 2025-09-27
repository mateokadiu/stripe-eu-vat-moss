package io.github.mateokadiu.moss.enrich.currency;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class EcbDailyFeedParserTest {

  // The actual ECB feed shape, simplified.
  private static final String SAMPLE =
      """
      <?xml version="1.0" encoding="UTF-8"?>
      <gesmes:Envelope xmlns:gesmes="http://www.gesmes.org/xml/2002-08-01"
                       xmlns="http://www.ecb.int/vocabulary/2002-08-01/eurofxref">
        <gesmes:subject>Reference rates</gesmes:subject>
        <gesmes:Sender><gesmes:name>European Central Bank</gesmes:name></gesmes:Sender>
        <Cube>
          <Cube time="2026-07-15">
            <Cube currency="USD" rate="1.0825"/>
            <Cube currency="JPY" rate="170.32"/>
            <Cube currency="GBP" rate="0.8412"/>
          </Cube>
        </Cube>
      </gesmes:Envelope>
      """;

  @Test
  void parsesEcbDailyFeed() {
    var rates = new EcbDailyFeedParser().parse(SAMPLE);

    assertThat(rates).hasSize(3);
    assertThat(rates.get(0).date()).isEqualTo(LocalDate.of(2026, 7, 15));
    assertThat(rates.get(0).fromCurrency()).isEqualTo("EUR");
    assertThat(rates.get(0).toCurrency()).isEqualTo("USD");
    assertThat(rates.get(0).rate()).isEqualByComparingTo(new BigDecimal("1.0825"));
  }
}
