package io.github.mateokadiu.moss.file.corrections;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.mateokadiu.moss.shared.Country;
import io.github.mateokadiu.moss.shared.Iso4217Currency;
import io.github.mateokadiu.moss.shared.Money;
import io.github.mateokadiu.moss.shared.Period;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CorrectionsRouterTest {

  @Test
  void encodesRule_GuidelinesSec3_6_refundReportedInPeriodWhenIssued() {
    Clock issuedQ3 = Clock.fixed(Instant.parse("2026-08-15T10:00:00Z"), ZoneOffset.UTC);
    var router = new CorrectionsRouter(issuedQ3);

    var event =
        router
            .route(
                UUID.randomUUID(),
                Period.of(2026, 1), // originally sold in Q1
                Country.of("DE"),
                Money.of(-10_000L, Iso4217Currency.EUR), // refunded EUR 100
                CorrectionReason.REFUND)
            .orElseThrow();

    assertThat(event.originalPeriod()).isEqualTo(Period.of(2026, 1));
    assertThat(event.reportingPeriod()).isEqualTo(Period.of(2026, 3));
    assertThat(event.reason()).isEqualTo(CorrectionReason.REFUND);
  }

  @Test
  void inPeriodAdjustmentReturnsEmpty() {
    Clock sameQ3 = Clock.fixed(Instant.parse("2026-09-15T10:00:00Z"), ZoneOffset.UTC);
    var router = new CorrectionsRouter(sameQ3);

    var event =
        router.route(
            UUID.randomUUID(),
            Period.of(2026, 3), // refund in same Q3
            Country.of("DE"),
            Money.of(-5_000L, Iso4217Currency.EUR),
            CorrectionReason.REFUND);

    assertThat(event).isEmpty();
  }

  @Test
  void rateChangeRoutesToCurrentPeriod() {
    Clock q4 = Clock.fixed(Instant.parse("2026-11-01T10:00:00Z"), ZoneOffset.UTC);
    var router = new CorrectionsRouter(q4);

    var event =
        router
            .route(
                UUID.randomUUID(),
                Period.of(2026, 2),
                Country.of("FI"),
                Money.of(-200L, Iso4217Currency.EUR),
                CorrectionReason.RATE_CHANGE)
            .orElseThrow();

    assertThat(event.reportingPeriod()).isEqualTo(Period.of(2026, 4));
    assertThat(event.reason()).isEqualTo(CorrectionReason.RATE_CHANGE);
  }
}
