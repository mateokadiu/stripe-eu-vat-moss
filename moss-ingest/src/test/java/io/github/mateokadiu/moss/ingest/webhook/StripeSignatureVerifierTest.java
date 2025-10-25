package io.github.mateokadiu.moss.ingest.webhook;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class StripeSignatureVerifierTest {

  private static final String SECRET = "whsec_testkey";
  private static final String PAYLOAD = "{\"id\":\"evt_001\",\"type\":\"tax.transaction.created\"}";

  @Test
  void acceptsValidSignature() {
    long ts = 1_724_000_000L;
    String hmac = StripeSignatureVerifier.hmacSha256Hex(SECRET, ts + "." + PAYLOAD);
    String header = "t=" + ts + ",v1=" + hmac;
    var verifier = new StripeSignatureVerifier(SECRET);

    assertThat(verifier.verify(PAYLOAD, header, Instant.ofEpochSecond(ts + 1))).isTrue();
  }

  @Test
  void rejectsWrongHmac() {
    long ts = 1_724_000_000L;
    String header = "t=" + ts + ",v1=" + "0".repeat(64);
    var verifier = new StripeSignatureVerifier(SECRET);

    assertThat(verifier.verify(PAYLOAD, header, Instant.ofEpochSecond(ts))).isFalse();
  }

  @Test
  void rejectsExpiredTimestamp() {
    long ts = 1_724_000_000L;
    String hmac = StripeSignatureVerifier.hmacSha256Hex(SECRET, ts + "." + PAYLOAD);
    String header = "t=" + ts + ",v1=" + hmac;
    var verifier = new StripeSignatureVerifier(SECRET, Duration.ofMinutes(5));

    // 10 minutes later: out of tolerance
    assertThat(verifier.verify(PAYLOAD, header, Instant.ofEpochSecond(ts + 600))).isFalse();
  }

  @Test
  void rejectsMissingHeader() {
    var verifier = new StripeSignatureVerifier(SECRET);
    assertThat(verifier.verify(PAYLOAD, null, Instant.now())).isFalse();
    assertThat(verifier.verify(PAYLOAD, "", Instant.now())).isFalse();
  }

  @Test
  void acceptsMultipleV1Schemes() {
    long ts = 1_724_000_000L;
    String hmac = StripeSignatureVerifier.hmacSha256Hex(SECRET, ts + "." + PAYLOAD);
    String header = "t=" + ts + ",v1=" + "0".repeat(64) + ",v1=" + hmac;
    var verifier = new StripeSignatureVerifier(SECRET);

    assertThat(verifier.verify(PAYLOAD, header, Instant.ofEpochSecond(ts))).isTrue();
  }
}
