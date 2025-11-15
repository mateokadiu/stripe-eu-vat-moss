package io.github.mateokadiu.moss.file.saf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.mateokadiu.moss.file.saf.SafOssMarshaller.XmlValidationException;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class SafOssMarshallerTest {

  private static final String MIN_XSD =
      """
      <?xml version="1.0" encoding="UTF-8"?>
      <xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema">
        <xs:element name="SAF-OSS">
          <xs:complexType>
            <xs:sequence>
              <xs:element name="Header">
                <xs:complexType>
                  <xs:sequence>
                    <xs:element name="FilerIdentification" type="xs:string"/>
                    <xs:element name="Period" type="xs:string"/>
                    <xs:element name="Currency" type="xs:string"/>
                  </xs:sequence>
                </xs:complexType>
              </xs:element>
              <xs:element name="Supplies" minOccurs="0">
                <xs:complexType>
                  <xs:sequence>
                    <xs:element name="Supply" minOccurs="0" maxOccurs="unbounded">
                      <xs:complexType>
                        <xs:sequence>
                          <xs:element name="MemberStateOfConsumption" type="xs:string"/>
                          <xs:element name="TaxableAmount" type="xs:decimal"/>
                          <xs:element name="VatRate" type="xs:decimal"/>
                          <xs:element name="VatAmount" type="xs:decimal"/>
                        </xs:sequence>
                      </xs:complexType>
                    </xs:element>
                  </xs:sequence>
                </xs:complexType>
              </xs:element>
              <xs:element name="Corrections" minOccurs="0">
                <xs:complexType>
                  <xs:sequence>
                    <xs:element name="Correction" minOccurs="0" maxOccurs="unbounded">
                      <xs:complexType>
                        <xs:sequence>
                          <xs:element name="OriginalPeriod" type="xs:string"/>
                          <xs:element name="MemberStateOfConsumption" type="xs:string"/>
                          <xs:element name="DeltaAmount" type="xs:decimal"/>
                        </xs:sequence>
                      </xs:complexType>
                    </xs:element>
                  </xs:sequence>
                </xs:complexType>
              </xs:element>
            </xs:sequence>
            <xs:attribute name="version" type="xs:string"/>
          </xs:complexType>
        </xs:element>
      </xs:schema>
      """;

  @Test
  void marshalsAndSelfHashes() {
    var doc =
        new SafOss(new Header("BE0123", "2026Q3", "EUR"))
            .withSupplies(
                List.of(
                    new Supply(
                        "DE",
                        new BigDecimal("100.00"),
                        new BigDecimal("19.0"),
                        new BigDecimal("19.00"))));

    var result = new SafOssMarshaller().marshal(doc);

    assertThat(result.xmlAsString()).contains("<SAF-OSS").contains("<Header>");
    assertThat(result.sha256()).hasSize(64); // hex sha-256
  }

  @Test
  void differentDocsHaveDifferentHashes() {
    var marshaller = new SafOssMarshaller();
    var a = marshaller.marshal(new SafOss(new Header("BE0123", "2026Q3", "EUR")));
    var b = marshaller.marshal(new SafOss(new Header("BE0999", "2026Q3", "EUR")));

    assertThat(a.sha256()).isNotEqualTo(b.sha256());
  }

  @Test
  void passesXsdValidationWhenValid() {
    var doc =
        new SafOss(new Header("BE0123", "2026Q3", "EUR"))
            .withSupplies(
                List.of(
                    new Supply(
                        "DE",
                        new BigDecimal("100.00"),
                        new BigDecimal("19.0"),
                        new BigDecimal("19.00"))));

    var marshaller =
        SafOssMarshaller.withXsd(MIN_XSD.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    var result = marshaller.marshal(doc);

    assertThat(result.xmlAsString()).contains("<TaxableAmount>100.00</TaxableAmount>");
  }

  @Test
  void rejectsInvalidXsd() {
    assertThatThrownBy(() -> SafOssMarshaller.withXsd("<not-xsd/>".getBytes()))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void emptyXmlMarshalsCleanly() {
    var doc = new SafOss(new Header("BE0", "2026Q3", "EUR"));

    var result = new SafOssMarshaller().marshal(doc);

    // No exception. Supplies and Corrections wrappers should be absent (or empty) — JAXB omits the
    // wrapper element when the list is empty.
    assertThat(result.xmlAsString()).contains("<SAF-OSS");
  }

  @Test
  void xmlValidationFailureSurfacesAsRuntimeException() {
    // A schema that requires a TaxableAmount on Supply but we emit Supply with null fields.
    String strictXsd =
        """
        <?xml version="1.0" encoding="UTF-8"?>
        <xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema">
          <xs:element name="SAF-OSS">
            <xs:complexType>
              <xs:sequence>
                <xs:element name="Header">
                  <xs:complexType>
                    <xs:sequence>
                      <xs:element name="FilerIdentification" type="xs:string"/>
                      <xs:element name="Period" type="xs:string"/>
                      <xs:element name="Currency" type="xs:string"/>
                      <xs:element name="MandatoryExtra" type="xs:string"/>
                    </xs:sequence>
                  </xs:complexType>
                </xs:element>
              </xs:sequence>
            </xs:complexType>
          </xs:element>
        </xs:schema>
        """;
    var marshaller =
        SafOssMarshaller.withXsd(strictXsd.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    var doc = new SafOss(new Header("BE0", "2026Q3", "EUR"));

    assertThatThrownBy(() -> marshaller.marshal(doc))
        .isInstanceOfAny(XmlValidationException.class, RuntimeException.class);
  }
}
