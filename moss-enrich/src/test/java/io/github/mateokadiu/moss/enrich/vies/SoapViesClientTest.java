package io.github.mateokadiu.moss.enrich.vies;

import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.github.mateokadiu.moss.shared.Country;
import java.net.URI;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SoapViesClientTest {

  private WireMockServer server;
  private SoapViesClient client;

  @BeforeEach
  void start() {
    server = new WireMockServer(options().dynamicPort());
    server.start();
    client = new SoapViesClient(URI.create("http://localhost:" + server.port() + "/checkVat"));
  }

  @AfterEach
  void stop() {
    server.stop();
  }

  @Test
  void parsesValidResponse() {
    String response =
        """
        <?xml version="1.0" encoding="UTF-8"?>
        <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
          <soapenv:Body>
            <checkVatResponse xmlns="urn:ec.europa.eu:taxud:vies:services:checkVat:types">
              <countryCode>BE</countryCode>
              <vatNumber>0123456789</vatNumber>
              <valid>true</valid>
              <name>Acme NV</name>
              <address>Rue de la Loi 1, 1000 Brussels</address>
            </checkVatResponse>
          </soapenv:Body>
        </soapenv:Envelope>
        """;
    server.stubFor(
        post(urlEqualTo("/checkVat"))
            .withHeader("Content-Type", matching("text/xml.*"))
            .willReturn(ok(response).withHeader("Content-Type", "text/xml")));

    var result = client.check(Country.of("BE"), "0123456789");

    assertThat(result.valid()).isTrue();
    assertThat(result.traderName()).contains("Acme NV");
    assertThat(result.traderAddress()).contains("Rue de la Loi 1, 1000 Brussels");
  }

  @Test
  void parsesInvalidResponse() {
    String response =
        """
        <?xml version="1.0" encoding="UTF-8"?>
        <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
          <soapenv:Body>
            <checkVatResponse xmlns="urn:ec.europa.eu:taxud:vies:services:checkVat:types">
              <countryCode>BE</countryCode>
              <vatNumber>0000000000</vatNumber>
              <valid>false</valid>
              <name>---</name>
              <address>---</address>
            </checkVatResponse>
          </soapenv:Body>
        </soapenv:Envelope>
        """;
    server.stubFor(post(urlEqualTo("/checkVat")).willReturn(ok(response)));

    var result = client.check(Country.of("BE"), "0000000000");

    assertThat(result.valid()).isFalse();
    assertThat(result.traderName()).isEmpty();
  }

  @Test
  void rejectsUnsafeVatNumber() {
    assertThatThrownBy(() -> client.check(Country.of("DE"), "<script>"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void surfacesUpstream500AsUnavailable() {
    server.stubFor(post(urlEqualTo("/checkVat")).willReturn(serverError()));

    assertThatThrownBy(() -> client.check(Country.of("BE"), "0123456789"))
        .isInstanceOf(ViesClient.ViesUnavailableException.class);
  }

  @Test
  void postsCountryAndVatNumberInEnvelope() {
    server.stubFor(
        post(urlEqualTo("/checkVat"))
            .willReturn(
                ok(
                    "<Envelope><Body><checkVatResponse><valid>true</valid></checkVatResponse></Body></Envelope>")));

    client.check(Country.of("DE"), "123456789");

    server.verify(
        postRequestedFor(urlEqualTo("/checkVat"))
            .withRequestBody(matching("(?s).*<urn:countryCode>DE</urn:countryCode>.*"))
            .withRequestBody(matching("(?s).*<urn:vatNumber>123456789</urn:vatNumber>.*"))
            .withHeader("Content-Type", matching("text/xml; charset=[uU][tT][fF]-8")));
  }
}
