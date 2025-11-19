package io.github.mateokadiu.moss.file.corrections;

import io.github.mateokadiu.moss.shared.Country;
import io.github.mateokadiu.moss.shared.Money;
import io.github.mateokadiu.moss.shared.Period;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Routes refunds and prior-period revisions into the correct reporting period.
 *
 * <p>EU rules encoded here:
 *
 * <ul>
 *   <li>Refunds are reflected in the period when they were <em>issued</em>, not the original sale
 *       period (OSS Guidelines §3.6).
 *   <li>Corrections to prior returns go into the next current return's {@code <Corrections>} block
 *       (never retroactive amendments).
 * </ul>
 */
public final class CorrectionsRouter {

  private final Clock clock;

  public CorrectionsRouter(Clock clock) {
    this.clock = clock;
  }

  /**
   * Build a correction event. If the supplied "now" is in the same period as the original sale, we
   * are amending in-period and there is no correction needed — callers should adjust the original
   * line instead.
   *
   * @return {@link Optional#empty()} if the situation is in-period; a {@link CorrectionEvent}
   *     otherwise.
   */
  public Optional<CorrectionEvent> route(
      UUID sourceTransactionId,
      Period originalPeriod,
      Country memberStateOfConsumption,
      Money delta,
      CorrectionReason reason) {
    Instant now = clock.instant();
    Period periodAtIssuance = Period.fromInstantUtc(now);
    if (periodAtIssuance.compareTo(originalPeriod) <= 0) {
      // refund/correction during the original period — no correction row, just adjust in place
      return Optional.empty();
    }
    return Optional.of(
        new CorrectionEvent(
            sourceTransactionId,
            originalPeriod,
            periodAtIssuance,
            memberStateOfConsumption,
            delta,
            reason,
            now));
  }
}
