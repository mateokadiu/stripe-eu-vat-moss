package io.github.mateokadiu.moss.file.corrections;

import io.github.mateokadiu.moss.shared.Country;
import io.github.mateokadiu.moss.shared.Money;
import io.github.mateokadiu.moss.shared.Period;
import java.time.Instant;
import java.util.UUID;

/**
 * A correction event: "we found out X happened that affects period P".
 *
 * <p>The reporting period is set by {@link CorrectionsRouter} based on when we became aware of the
 * event — never retroactively.
 */
public record CorrectionEvent(
    UUID sourceTransactionId,
    Period originalPeriod,
    Period reportingPeriod,
    Country memberStateOfConsumption,
    Money delta,
    CorrectionReason reason,
    Instant raisedAt) {}
