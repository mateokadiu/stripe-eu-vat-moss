package io.github.mateokadiu.moss.file.saf;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

/** SAF-OSS header — identifies the filer, the period, and the filing currency. */
@XmlAccessorType(XmlAccessType.FIELD)
public final class Header {

  @XmlElement(name = "FilerIdentification", required = true)
  private String filerIdentification;

  @XmlElement(name = "Period", required = true)
  private String period;

  @XmlElement(name = "Currency", required = true)
  private String currency;

  public Header() {}

  public Header(String filerIdentification, String period, String currency) {
    this.filerIdentification = filerIdentification;
    this.period = period;
    this.currency = currency;
  }

  public String filerIdentification() {
    return filerIdentification;
  }

  public String period() {
    return period;
  }

  public String currency() {
    return currency;
  }
}
