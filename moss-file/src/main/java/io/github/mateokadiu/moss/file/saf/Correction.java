package io.github.mateokadiu.moss.file.saf;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import java.math.BigDecimal;

/**
 * A correction line — adjustment to a prior period's filing.
 *
 * <p>Per OSS Guidelines §3.6, corrections are reported in the next current return, never as
 * retroactive amendments to the originating return.
 */
@XmlAccessorType(XmlAccessType.FIELD)
public final class Correction {

  @XmlElement(name = "OriginalPeriod", required = true)
  private String originalPeriod;

  @XmlElement(name = "MemberStateOfConsumption", required = true)
  private String memberStateOfConsumption;

  @XmlElement(name = "DeltaAmount", required = true)
  private BigDecimal deltaAmount;

  public Correction() {}

  public Correction(
      String originalPeriod, String memberStateOfConsumption, BigDecimal deltaAmount) {
    this.originalPeriod = originalPeriod;
    this.memberStateOfConsumption = memberStateOfConsumption;
    this.deltaAmount = deltaAmount;
  }

  public String originalPeriod() {
    return originalPeriod;
  }

  public String memberStateOfConsumption() {
    return memberStateOfConsumption;
  }

  public BigDecimal deltaAmount() {
    return deltaAmount;
  }
}
