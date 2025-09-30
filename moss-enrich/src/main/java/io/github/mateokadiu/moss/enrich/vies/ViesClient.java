package io.github.mateokadiu.moss.enrich.vies;

import io.github.mateokadiu.moss.shared.Country;

/** Client for the EU VIES VAT validation service. */
public interface ViesClient {

  /**
   * Validate a VAT number against the official VIES SOAP endpoint. Implementations should NOT do
   * their own caching — callers (e.g. {@link CachedViesValidator}) take care of that.
   */
  ViesCheckResult check(Country country, String vatNumber);

  /** Thrown when the upstream VIES service is unavailable or returned an error. */
  class ViesUnavailableException extends RuntimeException {
    public ViesUnavailableException(String message) {
      super(message);
    }

    public ViesUnavailableException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
