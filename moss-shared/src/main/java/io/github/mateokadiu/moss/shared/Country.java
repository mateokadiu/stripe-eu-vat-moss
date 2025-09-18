package io.github.mateokadiu.moss.shared;

import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * An ISO 3166-1 alpha-2 country code (e.g. {@code BE}, {@code DE}). Validated against the JDK's
 * locale registry at construction.
 */
public record Country(String code) {

  private static final Set<String> ISO_COUNTRIES =
      Arrays.stream(Locale.getISOCountries()).collect(Collectors.toUnmodifiableSet());

  /**
   * The 27 EU member states. EU membership changes infrequently — when it does, this set is the
   * single place to update.
   */
  public static final Set<String> EU_MEMBER_STATES =
      Set.of(
          "AT", "BE", "BG", "CY", "CZ", "DE", "DK", "EE", "ES", "FI", "FR", "GR", "HR", "HU", "IE",
          "IT", "LT", "LU", "LV", "MT", "NL", "PL", "PT", "RO", "SE", "SI", "SK");

  public Country {
    Objects.requireNonNull(code, "code");
    if (code.length() != 2) {
      throw new IllegalArgumentException("country code must be 2 letters: " + code);
    }
    String upper = code.toUpperCase(Locale.ROOT);
    if (!ISO_COUNTRIES.contains(upper)) {
      throw new IllegalArgumentException("unknown ISO 3166-1 alpha-2 country code: " + code);
    }
    if (!upper.equals(code)) {
      throw new IllegalArgumentException("country code must be uppercase: " + code);
    }
  }

  public static Country of(String code) {
    return new Country(code);
  }

  public boolean isEuMemberState() {
    return EU_MEMBER_STATES.contains(code);
  }
}
