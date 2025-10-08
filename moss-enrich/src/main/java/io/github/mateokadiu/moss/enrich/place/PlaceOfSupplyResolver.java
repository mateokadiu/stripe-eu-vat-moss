package io.github.mateokadiu.moss.enrich.place;

import io.github.mateokadiu.moss.enrich.evidence.EvidenceBundle;
import io.github.mateokadiu.moss.shared.Country;
import io.github.mateokadiu.moss.shared.Money;
import java.util.Objects;

/**
 * Resolves the place of supply for OSS purposes.
 *
 * <p>Rule encoded here: Council Directive 2017/2455 Art. 59c — above the EUR 10,000 EU-wide
 * cross-border B2C threshold, place of supply is the customer's MS of consumption. Below the
 * threshold, place of supply may be the supplier's MS (operator-configurable).
 */
public final class PlaceOfSupplyResolver {

  private final Country supplierCountry;
  private final boolean smallEnterprise;

  public PlaceOfSupplyResolver(Country supplierCountry, boolean smallEnterprise) {
    this.supplierCountry = Objects.requireNonNull(supplierCountry, "supplierCountry");
    this.smallEnterprise = smallEnterprise;
  }

  /**
   * Resolve where the supply takes place.
   *
   * @param crossBorderB2CRunningTotal aggregate EU cross-border B2C turnover this year so far
   * @param customerEvidence bundle of evidence pieces for this transaction's location
   * @param customerIsBusinessWithValidVat true if a valid B2B VAT was confirmed via VIES — in that
   *     case the supply is reverse-charge and the customer's MS is the place of supply on paper but
   *     the supplier collects no VAT
   */
  public PlaceOfSupplyDecision resolve(
      Money crossBorderB2CRunningTotal,
      EvidenceBundle customerEvidence,
      boolean customerIsBusinessWithValidVat) {
    if (customerIsBusinessWithValidVat) {
      return new PlaceOfSupplyDecision(
          customerEvidence.agreedCountry().orElse(supplierCountry),
          SupplyClassification.B2B_REVERSE_CHARGE,
          "reverse-charge");
    }

    long minorTen_k =
        10_000L
            * Math.round(
                Math.pow(10, crossBorderB2CRunningTotal.currency().defaultFractionDigits()));
    boolean aboveThreshold = crossBorderB2CRunningTotal.minorUnits() >= minorTen_k;

    if (!aboveThreshold && !smallEnterprise) {
      // below threshold: supplier-MS rules — place of supply is supplier country
      return new PlaceOfSupplyDecision(
          supplierCountry, SupplyClassification.B2C_DOMESTIC, "below EUR 10,000 threshold");
    }

    if (!customerEvidence.isSufficient(smallEnterprise)) {
      return new PlaceOfSupplyDecision(
          customerEvidence.agreedCountry().orElse(supplierCountry),
          SupplyClassification.NEEDS_REVIEW,
          "insufficient or conflicting evidence");
    }
    return new PlaceOfSupplyDecision(
        customerEvidence.agreedCountry().orElseThrow(),
        SupplyClassification.B2C_CROSS_BORDER,
        "above threshold, evidence agrees");
  }

  /** Outcome of a resolution. */
  public record PlaceOfSupplyDecision(
      Country memberStateOfConsumption, SupplyClassification classification, String rationale) {}

  /** What kind of supply this is for OSS reporting purposes. */
  public enum SupplyClassification {
    B2C_DOMESTIC,
    B2C_CROSS_BORDER,
    B2B_REVERSE_CHARGE,
    NEEDS_REVIEW
  }
}
