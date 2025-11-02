package io.github.mateokadiu.moss.file.saf;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import java.math.BigDecimal;

/** A single Supply line — (MS-of-consumption, VAT rate) summary in the return. */
@XmlAccessorType(XmlAccessType.FIELD)
public final class Supply {

  @XmlElement(name = "MemberStateOfConsumption", required = true)
  private String memberStateOfConsumption;

  @XmlElement(name = "TaxableAmount", required = true)
  private BigDecimal taxableAmount;

  @XmlElement(name = "VatRate", required = true)
  private BigDecimal vatRate;

  @XmlElement(name = "VatAmount", required = true)
  private BigDecimal vatAmount;

  public Supply() {}

  public Supply(
      String memberStateOfConsumption,
      BigDecimal taxableAmount,
      BigDecimal vatRate,
      BigDecimal vatAmount) {
    this.memberStateOfConsumption = memberStateOfConsumption;
    this.taxableAmount = taxableAmount;
    this.vatRate = vatRate;
    this.vatAmount = vatAmount;
  }

  public String memberStateOfConsumption() {
    return memberStateOfConsumption;
  }

  public BigDecimal taxableAmount() {
    return taxableAmount;
  }

  public BigDecimal vatRate() {
    return vatRate;
  }

  public BigDecimal vatAmount() {
    return vatAmount;
  }
}
