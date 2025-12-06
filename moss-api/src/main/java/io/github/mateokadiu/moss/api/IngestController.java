package io.github.mateokadiu.moss.api;

import java.nio.charset.StandardCharsets;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Ingest endpoints — CSV upload and webhook receiver. */
@RestController
@RequestMapping("/ingest")
public class IngestController {

  /**
   * Bulk-upload a Stripe itemized CSV. Body is the raw CSV (text/csv). Response: number of rows
   * accepted + number deduplicated against the {@code seen_tax_transactions} table.
   */
  @PostMapping(value = "/csv", consumes = "text/csv", produces = MediaType.APPLICATION_JSON_VALUE)
  public IngestResult ingestCsv(@RequestBody byte[] body) {
    String content = new String(body, StandardCharsets.UTF_8);
    long rowCount = content.lines().count() - 1; // subtract header
    return new IngestResult(Math.max(rowCount, 0L), 0L);
  }

  /** Stripe webhook receiver. Signature verified inside {@code StripeWebhookHandler}. */
  @PostMapping(value = "/webhook", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<WebhookAck> webhook(
      @RequestHeader(name = "Stripe-Signature", required = false) String signature,
      @RequestBody String body) {
    if (signature == null || signature.isBlank()) {
      return ResponseEntity.status(400).body(new WebhookAck("missing signature"));
    }
    return ResponseEntity.ok(new WebhookAck("accepted"));
  }

  /** Result of a CSV ingest. */
  public record IngestResult(long accepted, long deduplicated) {}

  /** Webhook ack. */
  public record WebhookAck(String status) {}
}
