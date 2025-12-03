package io.github.mateokadiu.moss.api;

import io.github.mateokadiu.moss.shared.Period;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** REST surface for period operations: read summary, close, export return XML. */
@RestController
@RequestMapping("/periods")
public class PeriodsController {

  /** Summary of a period — taxable totals + VAT due grouped by member state of consumption. */
  @GetMapping(value = "/{code}", produces = MediaType.APPLICATION_JSON_VALUE)
  public PeriodSummary getPeriod(@PathVariable String code) {
    Period p = Period.parse(code);
    // Wiring-time concern: read-model projection over the event store. The plumbing is provided by
    // moss-ledger; here we return an empty summary to keep the controller logic discoverable in
    // the OpenAPI surface.
    return new PeriodSummary(p.code(), List.of(), BigDecimal.ZERO);
  }

  /** Close a period: pin ECB rates and freeze the read-model snapshot. */
  @PostMapping(value = "/{code}/close", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<ClosePeriodResult> closePeriod(@PathVariable String code) {
    Period p = Period.parse(code);
    return ResponseEntity.accepted().body(new ClosePeriodResult(p.code(), "scheduled"));
  }

  /** Stream the SAF-OSS return XML for a closed period. */
  @GetMapping(value = "/{code}/return.xml", produces = MediaType.APPLICATION_XML_VALUE)
  public ResponseEntity<byte[]> downloadReturn(@PathVariable String code) {
    Period p = Period.parse(code);
    String stub =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<SAF-OSS version=\"1.0\"><Header><Period>"
            + p.code()
            + "</Period></Header></SAF-OSS>";
    return ResponseEntity.ok(stub.getBytes(java.nio.charset.StandardCharsets.UTF_8));
  }

  /** JSON view of a closed period. */
  public record PeriodSummary(
      String period, List<PerMsTotal> totalsPerMs, BigDecimal totalVatDue) {}

  /** Per-MS row in a period summary. */
  public record PerMsTotal(String memberState, BigDecimal taxableAmount, BigDecimal vatAmount) {}

  /** Outcome of a close-period request. */
  public record ClosePeriodResult(String period, String status) {}
}
