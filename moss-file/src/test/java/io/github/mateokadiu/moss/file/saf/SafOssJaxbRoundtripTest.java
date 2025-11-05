package io.github.mateokadiu.moss.file.saf;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class SafOssJaxbRoundtripTest {

  @Test
  void marshalsAndUnmarshalsRootElement() throws Exception {
    var doc =
        new SafOss(new Header("BE0123456789", "2026Q3", "EUR"))
            .withSupplies(
                List.of(
                    new Supply(
                        "DE",
                        new BigDecimal("12500.00"),
                        new BigDecimal("19.0"),
                        new BigDecimal("2375.00")),
                    new Supply(
                        "FR",
                        new BigDecimal("3000.00"),
                        new BigDecimal("20.0"),
                        new BigDecimal("600.00"))))
            .withCorrections(
                List.of(new Correction("2026Q1", "FR", new BigDecimal("-450.00"))));

    JAXBContext ctx = JAXBContext.newInstance(SafOss.class);
    StringWriter writer = new StringWriter();
    Marshaller m = ctx.createMarshaller();
    m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
    m.marshal(doc, writer);

    String xml = writer.toString();
    assertThat(xml).contains("<SAF-OSS").contains("<Header>");
    assertThat(xml).contains("<MemberStateOfConsumption>DE</MemberStateOfConsumption>");
    assertThat(xml).contains("<OriginalPeriod>2026Q1</OriginalPeriod>");
    assertThat(xml).contains("version=\"1.0\"");

    Unmarshaller u = ctx.createUnmarshaller();
    SafOss back = (SafOss) u.unmarshal(new StringReader(xml));

    assertThat(back.header().filerIdentification()).isEqualTo("BE0123456789");
    assertThat(back.header().period()).isEqualTo("2026Q3");
    assertThat(back.supplies()).hasSize(2);
    assertThat(back.supplies().get(0).memberStateOfConsumption()).isEqualTo("DE");
    assertThat(back.corrections()).hasSize(1);
  }
}
