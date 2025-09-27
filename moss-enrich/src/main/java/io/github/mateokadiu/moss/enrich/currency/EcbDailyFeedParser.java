package io.github.mateokadiu.moss.enrich.currency;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Parses the ECB daily euro reference rate XML feed.
 *
 * <p>The feed format is published at
 * https://www.ecb.europa.eu/stats/policy_and_exchange_rates/euro_reference_exchange_rates/html/index.en.html
 * and looks like:
 *
 * <pre>{@code
 * <gesmes:Envelope ...>
 *   <Cube>
 *     <Cube time="2026-07-15">
 *       <Cube currency="USD" rate="1.0825"/>
 *       <Cube currency="JPY" rate="170.32"/>
 *       ...
 *     </Cube>
 *   </Cube>
 * </gesmes:Envelope>
 * }</pre>
 *
 * Rates are quoted as EUR -> X (i.e. how many X for 1 EUR).
 */
public final class EcbDailyFeedParser {

  private final DocumentBuilderFactory factory;

  public EcbDailyFeedParser() {
    this.factory = DocumentBuilderFactory.newInstance();
    try {
      factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
      factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
      factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
      factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
    } catch (ParserConfigurationException ex) {
      throw new IllegalStateException("could not lock down xml parser", ex);
    }
    factory.setNamespaceAware(true);
    factory.setXIncludeAware(false);
    factory.setExpandEntityReferences(false);
  }

  public List<EcbDailyRate> parse(byte[] xml) {
    try {
      DocumentBuilder builder = factory.newDocumentBuilder();
      try (InputStream in = new ByteArrayInputStream(xml)) {
        Document doc = builder.parse(in);
        return collect(doc);
      }
    } catch (ParserConfigurationException | SAXException | java.io.IOException ex) {
      throw new IllegalStateException("could not parse ECB XML feed", ex);
    }
  }

  private List<EcbDailyRate> collect(Document doc) {
    List<EcbDailyRate> out = new ArrayList<>();
    NodeList allCubes = doc.getElementsByTagNameNS("*", "Cube");
    for (int i = 0; i < allCubes.getLength(); i++) {
      Node n = allCubes.item(i);
      if (n.getNodeType() != Node.ELEMENT_NODE) {
        continue;
      }
      Element e = (Element) n;
      String time = e.getAttribute("time");
      if (time.isEmpty()) {
        continue;
      }
      LocalDate date = LocalDate.parse(time);
      NodeList children = e.getChildNodes();
      for (int j = 0; j < children.getLength(); j++) {
        Node child = children.item(j);
        if (child.getNodeType() != Node.ELEMENT_NODE) {
          continue;
        }
        Element re = (Element) child;
        String currency = re.getAttribute("currency");
        String rateStr = re.getAttribute("rate");
        if (currency.isEmpty() || rateStr.isEmpty()) {
          continue;
        }
        out.add(new EcbDailyRate(date, "EUR", currency, new BigDecimal(rateStr)));
      }
    }
    return out;
  }

  /** Convenience: parse a UTF-8 string. */
  public List<EcbDailyRate> parse(String xml) {
    return parse(xml.getBytes(StandardCharsets.UTF_8));
  }

  /** One parsed feed entry. */
  public record EcbDailyRate(
      LocalDate date, String fromCurrency, String toCurrency, BigDecimal rate) {}
}
