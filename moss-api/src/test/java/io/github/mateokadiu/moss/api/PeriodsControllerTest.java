package io.github.mateokadiu.moss.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class PeriodsControllerTest {

  private final PeriodsController controller = new PeriodsController();

  @Test
  void readsPeriodSummary() {
    var summary = controller.getPeriod("2026Q3");

    assertThat(summary.period()).isEqualTo("2026Q3");
    assertThat(summary.totalsPerMs()).isEmpty();
  }

  @Test
  void closeAccepted() {
    var r = controller.closePeriod("2026Q3");

    assertThat(r.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
    assertThat(r.getBody().status()).isEqualTo("scheduled");
  }

  @Test
  void downloadsReturnXml() {
    var r = controller.downloadReturn("2026Q3");

    assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
    String body = new String(r.getBody(), java.nio.charset.StandardCharsets.UTF_8);
    assertThat(body).contains("<SAF-OSS").contains("<Period>2026Q3</Period>");
  }
}
