package io.github.mateokadiu.moss.ingest.csv;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import io.github.mateokadiu.moss.ingest.csv.StripeCsvRow.StripeCsvType;
import io.github.mateokadiu.moss.shared.Country;
import io.github.mateokadiu.moss.shared.Iso4217Currency;
import io.github.mateokadiu.moss.shared.Money;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Parses Stripe's itemized Tax CSV export. The header row gives us column ordering; column names
 * are matched case-insensitively. Streaming-friendly: the reader processes one row at a time.
 */
public final class StripeTaxCsvParser {

  /** Required columns. Parsing fails fast if any of these are missing. */
  private static final List<String> REQUIRED =
      List.of(
          "id",
          "tax_transaction_id",
          "type",
          "currency",
          "subtotal",
          "tax_amount",
          "transaction_date_utc",
          "country_code");

  public List<StripeCsvRow> parseAll(Reader reader) {
    List<StripeCsvRow> rows = new ArrayList<>();
    try (CSVReader csv = new CSVReader(new BufferedReader(reader))) {
      String[] header = csv.readNext();
      if (header == null) {
        return rows;
      }
      Map<String, Integer> idx = indexHeader(header);
      String[] line;
      while ((line = csv.readNext()) != null) {
        rows.add(parseRow(idx, line));
      }
      return rows;
    } catch (IOException | CsvException ex) {
      throw new IllegalArgumentException("could not parse Stripe CSV", ex);
    }
  }

  Map<String, Integer> indexHeader(String[] header) {
    Map<String, Integer> idx = new HashMap<>();
    for (int i = 0; i < header.length; i++) {
      idx.put(header[i].trim().toLowerCase(Locale.ROOT), i);
    }
    for (String required : REQUIRED) {
      if (!idx.containsKey(required)) {
        throw new IllegalArgumentException("CSV missing required column: " + required);
      }
    }
    return idx;
  }

  private StripeCsvRow parseRow(Map<String, Integer> idx, String[] line) {
    String currencyCode = at(line, idx, "currency").toUpperCase(Locale.ROOT);
    Iso4217Currency currency = Iso4217Currency.of(currencyCode);
    Money subtotal = asMoney(at(line, idx, "subtotal"), currency);
    Money tax = asMoney(at(line, idx, "tax_amount"), currency);
    return new StripeCsvRow(
        at(line, idx, "id"),
        at(line, idx, "tax_transaction_id"),
        parseType(at(line, idx, "type")),
        currency,
        subtotal,
        tax,
        parseInstant(at(line, idx, "transaction_date_utc")),
        Country.of(at(line, idx, "country_code").toUpperCase(Locale.ROOT)),
        opt(line, idx, "state_code"),
        opt(line, idx, "tax_rate"),
        opt(line, idx, "tax_name"),
        opt(line, idx, "customer_tax_id"),
        opt(line, idx, "origin_address"),
        opt(line, idx, "destination_address"));
  }

  private static String at(String[] line, Map<String, Integer> idx, String col) {
    Integer i = idx.get(col);
    if (i == null || i >= line.length) {
      throw new IllegalArgumentException("missing column: " + col);
    }
    String v = line[i];
    if (v == null) {
      throw new IllegalArgumentException("missing value for column: " + col);
    }
    return v.trim();
  }

  private static Optional<String> opt(String[] line, Map<String, Integer> idx, String col) {
    Integer i = idx.get(col);
    if (i == null || i >= line.length) {
      return Optional.empty();
    }
    String v = line[i];
    return (v == null || v.isBlank()) ? Optional.empty() : Optional.of(v.trim());
  }

  private static StripeCsvType parseType(String raw) {
    return switch (raw.toLowerCase(Locale.ROOT)) {
      case "sale", "charge" -> StripeCsvType.SALE;
      case "refund", "refunded" -> StripeCsvType.REFUND;
      default -> throw new IllegalArgumentException("unknown row type: " + raw);
    };
  }

  /**
   * Accepts both ISO-8601 (preferred) and Stripe's older "yyyy-MM-dd HH:mm:ss" UTC format used in
   * legacy exports.
   */
  static Instant parseInstant(String raw) {
    String trimmed = raw.trim();
    try {
      return Instant.parse(trimmed);
    } catch (Exception ignored) {
      // fall through to legacy format
    }
    try {
      return LocalDateTime.parse(trimmed, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
          .toInstant(ZoneOffset.UTC);
    } catch (Exception ignored) {
      // try plain date
    }
    return LocalDate.parse(trimmed).atStartOfDay(ZoneOffset.UTC).toInstant();
  }

  static Money asMoney(String raw, Iso4217Currency currency) {
    return Money.ofMajor(new BigDecimal(raw), currency);
  }

  /** Convenience for tests / one-shot parsing. */
  public List<StripeCsvRow> parseAll(byte[] bytes) {
    return parseAll(
        new java.io.InputStreamReader(
            new java.io.ByteArrayInputStream(bytes), StandardCharsets.UTF_8));
  }
}
