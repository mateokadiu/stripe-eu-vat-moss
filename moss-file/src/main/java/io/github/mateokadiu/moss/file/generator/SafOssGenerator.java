package io.github.mateokadiu.moss.file.generator;

import io.github.mateokadiu.moss.file.saf.Correction;
import io.github.mateokadiu.moss.file.saf.Header;
import io.github.mateokadiu.moss.file.saf.SafOss;
import io.github.mateokadiu.moss.file.saf.Supply;
import io.github.mateokadiu.moss.shared.Country;
import io.github.mateokadiu.moss.shared.Iso4217Currency;
import io.github.mateokadiu.moss.shared.Money;
import io.github.mateokadiu.moss.shared.Period;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Read-model -> {@link SafOss} document.
 *
 * <p>Group rules: supplies are grouped by (MS-of-consumption, VAT rate); corrections are grouped by
 * (original period, MS-of-consumption). Negative balances are NEVER netted across member states
 * (OSS Guidelines §3.6).
 */
public final class SafOssGenerator {

  private final String filerIdentification;
  private final Iso4217Currency filingCurrency;

  public SafOssGenerator(String filerIdentification, Iso4217Currency filingCurrency) {
    this.filerIdentification = Objects.requireNonNull(filerIdentification);
    this.filingCurrency = Objects.requireNonNull(filingCurrency);
  }

  /**
   * Build the return for {@code period} from {@code lines} (current-period supply lines) and {@code
   * corrections} (refunds and prior-period revisions routed into this period).
   */
  public SafOss build(Period period, List<SupplyLine> lines, List<CorrectionLine> corrections) {
    Map<SupplyKey, SupplyAccumulator> supplyMap = new HashMap<>();
    for (SupplyLine l : lines) {
      if (!l.taxableAmount().currency().equals(filingCurrency)) {
        throw new IllegalArgumentException(
            "currency mismatch — expected "
                + filingCurrency.code()
                + " got "
                + l.taxableAmount().currency().code());
      }
      SupplyKey key = new SupplyKey(l.memberStateOfConsumption(), l.vatRate());
      supplyMap.computeIfAbsent(key, k -> new SupplyAccumulator(filingCurrency)).add(l);
    }

    Map<CorrectionKey, Long> correctionMap = new HashMap<>();
    for (CorrectionLine c : corrections) {
      if (!c.delta().currency().equals(filingCurrency)) {
        throw new IllegalArgumentException(
            "correction currency mismatch — expected "
                + filingCurrency.code()
                + " got "
                + c.delta().currency().code());
      }
      CorrectionKey key = new CorrectionKey(c.originalPeriod(), c.memberStateOfConsumption());
      correctionMap.merge(key, c.delta().minorUnits(), Long::sum);
    }

    List<Supply> supplies = new ArrayList<>();
    supplyMap.entrySet().stream()
        .sorted(
            Comparator.<Map.Entry<SupplyKey, SupplyAccumulator>, String>comparing(
                    e -> e.getKey().memberState().code())
                .thenComparing(e -> e.getKey().vatRate()))
        .forEach(
            e ->
                supplies.add(
                    new Supply(
                        e.getKey().memberState().code(),
                        majorOf(e.getValue().taxable()),
                        e.getKey().vatRate(),
                        majorOf(e.getValue().vat()))));

    List<Correction> outCorrections = new ArrayList<>();
    correctionMap.entrySet().stream()
        .sorted(
            Comparator.<Map.Entry<CorrectionKey, Long>, String>comparing(
                    e -> e.getKey().originalPeriod().code())
                .thenComparing(e -> e.getKey().memberState().code()))
        .forEach(
            e ->
                outCorrections.add(
                    new Correction(
                        e.getKey().originalPeriod().code(),
                        e.getKey().memberState().code(),
                        majorOf(Money.of(e.getValue(), filingCurrency)))));

    return new SafOss(new Header(filerIdentification, period.code(), filingCurrency.code()))
        .withSupplies(supplies)
        .withCorrections(outCorrections);
  }

  private BigDecimal majorOf(Money m) {
    return m.toMajor().setScale(2, RoundingMode.HALF_UP);
  }

  /** One supply row at input — the read-model emits these per transaction-and-rate. */
  public record SupplyLine(
      Country memberStateOfConsumption, BigDecimal vatRate, Money taxableAmount, Money vatAmount) {}

  /** One correction row at input — routed from the corrections projector. */
  public record CorrectionLine(
      Period originalPeriod, Country memberStateOfConsumption, Money delta) {}

  private record SupplyKey(Country memberState, BigDecimal vatRate) {}

  private record CorrectionKey(Period originalPeriod, Country memberState) {}

  private static final class SupplyAccumulator {
    private Money taxable;
    private Money vat;

    SupplyAccumulator(Iso4217Currency currency) {
      this.taxable = Money.zero(currency);
      this.vat = Money.zero(currency);
    }

    void add(SupplyLine l) {
      this.taxable = this.taxable.plus(l.taxableAmount());
      this.vat = this.vat.plus(l.vatAmount());
    }

    Money taxable() {
      return taxable;
    }

    Money vat() {
      return vat;
    }
  }
}
