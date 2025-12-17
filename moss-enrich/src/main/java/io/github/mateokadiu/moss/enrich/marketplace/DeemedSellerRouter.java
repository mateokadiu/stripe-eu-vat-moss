package io.github.mateokadiu.moss.enrich.marketplace;

import java.util.Map;
import java.util.Objects;

/**
 * Routes a Stripe Connect transaction to either the platform or the underlying merchant for OSS
 * reporting purposes.
 *
 * <p>Rule encoded here: Council Directive 2017/2455 Art. 14a — when the platform is the deemed
 * seller, the platform files the OSS return for that transaction; the underlying merchant's leg is
 * zero-rated.
 *
 * <p>The default behaviour is operator-configurable via {@code MOSS_DEEMED_SELLER_DEFAULT}; the
 * per-transaction Stripe metadata key {@code deemed_seller} overrides it.
 */
public final class DeemedSellerRouter {

  static final String METADATA_KEY = "deemed_seller";
  static final String METADATA_PLATFORM = "platform";
  static final String METADATA_MERCHANT = "merchant";

  private final boolean defaultIsDeemedSeller;

  public DeemedSellerRouter(boolean defaultIsDeemedSeller) {
    this.defaultIsDeemedSeller = defaultIsDeemedSeller;
  }

  /** Decide where the OSS supply is filed from based on Stripe metadata on the charge. */
  public DeemedSellerDecision decide(
      Map<String, String> stripeMetadata, String underlyingMerchantId) {
    Objects.requireNonNull(stripeMetadata, "stripeMetadata");
    String override = stripeMetadata.get(METADATA_KEY);
    boolean isPlatform;
    if (METADATA_PLATFORM.equalsIgnoreCase(override)) {
      isPlatform = true;
    } else if (METADATA_MERCHANT.equalsIgnoreCase(override)) {
      isPlatform = false;
    } else {
      isPlatform = defaultIsDeemedSeller;
    }
    return new DeemedSellerDecision(isPlatform, underlyingMerchantId);
  }

  /**
   * Outcome.
   *
   * @param platformIsDeemedSeller true when the platform files the OSS supply for this transaction
   * @param underlyingMerchantId the connected account id whose leg is zero-rated when {@code
   *     platformIsDeemedSeller=true}
   */
  public record DeemedSellerDecision(boolean platformIsDeemedSeller, String underlyingMerchantId) {}
}
