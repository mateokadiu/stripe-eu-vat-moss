package io.github.mateokadiu.moss.enrich.vies;

import io.github.mateokadiu.moss.shared.Country;
import java.time.Instant;
import java.util.Optional;

/**
 * The outcome of a VIES VAT-number check. The raw XML response is kept for audit purposes — under
 * EU Council Directive 2006/112/EC Art. 196, a supplier must hold proof of the validation when
 * applying reverse charge.
 */
public record ViesCheckResult(
    Country country,
    String vatNumber,
    boolean valid,
    Optional<String> traderName,
    Optional<String> traderAddress,
    Instant checkedAt,
    String rawResponse) {}
