package io.github.mateokadiu.moss.file.saf;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

/**
 * The root element of a SAF-OSS return.
 *
 * <p>This class is the JAXB-bindable representation of the structure mandated by Commission
 * Implementing Regulation (EU) 2021/965. The element + attribute names match the published schema's
 * macro shape; XSD-validated bytes are produced only when an operator-supplied XSD is registered
 * with {@link SafOssMarshaller}.
 */
@XmlRootElement(name = "SAF-OSS")
@XmlAccessorType(XmlAccessType.FIELD)
public final class SafOss {

  @XmlAttribute(name = "version")
  private String version = "1.0";

  @XmlElement(name = "Header", required = true)
  private Header header;

  @XmlElementWrapper(name = "Supplies")
  @XmlElement(name = "Supply")
  private List<Supply> supplies = new ArrayList<>();

  @XmlElementWrapper(name = "Corrections")
  @XmlElement(name = "Correction")
  private List<Correction> corrections = new ArrayList<>();

  public SafOss() {}

  public SafOss(Header header) {
    this.header = header;
  }

  public Header header() {
    return header;
  }

  public List<Supply> supplies() {
    return supplies;
  }

  public List<Correction> corrections() {
    return corrections;
  }

  public String version() {
    return version;
  }

  public SafOss withSupplies(List<Supply> s) {
    this.supplies = new ArrayList<>(s);
    return this;
  }

  public SafOss withCorrections(List<Correction> c) {
    this.corrections = new ArrayList<>(c);
    return this;
  }
}
