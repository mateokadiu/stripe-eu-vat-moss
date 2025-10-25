package io.github.mateokadiu.moss.ingest.webhook;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Verifies the {@code Stripe-Signature} header.
 *
 * <p>Format: {@code t=<timestamp>,v1=<hex hmac-sha256>}. Replay window: 5 minutes by default
 * (matching Stripe's recommendation).
 *
 * <p>Encoded here intentionally — using Stripe's own {@code Webhook.constructEvent} pulls in JSON
 * deserialization we don't need; the raw payload is stored in {@code webhook_events} as JSONB.
 */
public final class StripeSignatureVerifier {

  private final String secret;
  private final Duration tolerance;

  public StripeSignatureVerifier(String secret) {
    this(secret, Duration.ofMinutes(5));
  }

  public StripeSignatureVerifier(String secret, Duration tolerance) {
    if (secret == null || secret.isBlank()) {
      throw new IllegalArgumentException("secret is required");
    }
    this.secret = secret;
    this.tolerance = tolerance;
  }

  public boolean verify(String payload, String header, Instant now) {
    if (header == null || header.isBlank()) {
      return false;
    }
    String ts = null;
    java.util.List<String> v1 = new java.util.ArrayList<>();
    for (String token : header.split(",")) {
      String[] kv = token.split("=", 2);
      if (kv.length != 2) {
        continue;
      }
      switch (kv[0].trim()) {
        case "t" -> ts = kv[1].trim();
        case "v1" -> v1.add(kv[1].trim());
        default -> {
          // ignore other schemes
        }
      }
    }
    if (ts == null || v1.isEmpty()) {
      return false;
    }
    long sentSeconds;
    try {
      sentSeconds = Long.parseLong(ts);
    } catch (NumberFormatException ex) {
      return false;
    }
    if (Math.abs(now.getEpochSecond() - sentSeconds) > tolerance.toSeconds()) {
      return false;
    }
    String signedPayload = ts + "." + payload;
    String expected = hmacSha256Hex(secret, signedPayload);
    for (String candidate : v1) {
      if (constantTimeEquals(expected, candidate)) {
        return true;
      }
    }
    return false;
  }

  static String hmacSha256Hex(String secret, String data) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash);
    } catch (Exception ex) {
      throw new IllegalStateException("could not compute HMAC", ex);
    }
  }

  private static boolean constantTimeEquals(String a, String b) {
    if (a.length() != b.length()) {
      return false;
    }
    int diff = 0;
    for (int i = 0; i < a.length(); i++) {
      diff |= a.charAt(i) ^ b.charAt(i);
    }
    return diff == 0;
  }
}
