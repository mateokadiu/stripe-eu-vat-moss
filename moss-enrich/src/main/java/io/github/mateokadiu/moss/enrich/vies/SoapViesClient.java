package io.github.mateokadiu.moss.enrich.vies;

import io.github.mateokadiu.moss.shared.Country;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

/**
 * HTTP-based VIES client. Talks to the SOAP endpoint with a tiny XML envelope. We avoid pulling in
 * a full SOAP stack (no JAX-WS) because the protocol surface here is two fields in, four fields
 * out.
 *
 * <p>Default endpoint: {@code https://ec.europa.eu/taxation_customs/vies/services/checkVatService}.
 */
public final class SoapViesClient implements ViesClient {

  /** XML escape — VIES rejects unescaped angle brackets in the VAT number field. */
  private static final Pattern UNSAFE = Pattern.compile("[<>&\"']");

  private final HttpClient http;
  private final URI endpoint;
  private final Duration timeout;

  public SoapViesClient() {
    this(URI.create("https://ec.europa.eu/taxation_customs/vies/services/checkVatService"));
  }

  public SoapViesClient(URI endpoint) {
    this(endpoint, HttpClient.newHttpClient(), Duration.ofSeconds(10));
  }

  public SoapViesClient(URI endpoint, HttpClient client, Duration timeout) {
    this.endpoint = endpoint;
    this.http = client;
    this.timeout = timeout;
  }

  @Override
  public ViesCheckResult check(Country country, String vatNumber) {
    if (UNSAFE.matcher(vatNumber).find()) {
      throw new IllegalArgumentException("VAT number contains unsafe characters: " + vatNumber);
    }
    String envelope =
        """
        <?xml version="1.0" encoding="UTF-8"?>
        <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                          xmlns:urn="urn:ec.europa.eu:taxud:vies:services:checkVat:types">
          <soapenv:Header/>
          <soapenv:Body>
            <urn:checkVat>
              <urn:countryCode>%s</urn:countryCode>
              <urn:vatNumber>%s</urn:vatNumber>
            </urn:checkVat>
          </soapenv:Body>
        </soapenv:Envelope>
        """
            .formatted(country.code(), vatNumber);

    HttpRequest req =
        HttpRequest.newBuilder()
            .uri(endpoint)
            .timeout(timeout)
            .header("Content-Type", "text/xml; charset=utf-8")
            .header("SOAPAction", "")
            .POST(HttpRequest.BodyPublishers.ofString(envelope, StandardCharsets.UTF_8))
            .build();

    HttpResponse<String> resp;
    try {
      resp = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    } catch (java.io.IOException | InterruptedException ex) {
      if (ex instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }
      throw new ViesUnavailableException("VIES request failed", ex);
    }
    if (resp.statusCode() >= 500) {
      throw new ViesUnavailableException("VIES returned status " + resp.statusCode());
    }
    return parse(country, vatNumber, resp.body());
  }

  ViesCheckResult parse(Country country, String vatNumber, String body) {
    boolean valid = body.contains("<valid>true</valid>");
    Optional<String> name = extract(body, "name");
    Optional<String> address = extract(body, "address");
    return new ViesCheckResult(country, vatNumber, valid, name, address, Instant.now(), body);
  }

  private Optional<String> extract(String body, String localName) {
    try {
      DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
      f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
      f.setNamespaceAware(true);
      DocumentBuilder b = f.newDocumentBuilder();
      Document doc = b.parse(new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)));
      NodeList nodes = doc.getElementsByTagNameNS("*", localName);
      if (nodes.getLength() == 0) {
        return Optional.empty();
      }
      String t = nodes.item(0).getTextContent();
      return (t == null || t.isBlank() || "---".equals(t.trim()))
          ? Optional.empty()
          : Optional.of(t.trim());
    } catch (ParserConfigurationException | org.xml.sax.SAXException | java.io.IOException ex) {
      return Optional.empty();
    }
  }
}
