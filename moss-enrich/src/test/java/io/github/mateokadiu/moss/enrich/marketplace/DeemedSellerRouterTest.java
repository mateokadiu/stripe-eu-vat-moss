package io.github.mateokadiu.moss.enrich.marketplace;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

class DeemedSellerRouterTest {

  @Test
  void encodesRule_Art14a_metadataOverridesPlatformDefault() {
    var router = new DeemedSellerRouter(false); // default = merchant files

    var byPlatform = router.decide(Map.of("deemed_seller", "platform"), "acct_x");
    var byMerchant = router.decide(Map.of("deemed_seller", "merchant"), "acct_x");

    assertThat(byPlatform.platformIsDeemedSeller()).isTrue();
    assertThat(byMerchant.platformIsDeemedSeller()).isFalse();
  }

  @Test
  void defaultIsAppliedWhenMetadataAbsent() {
    var platformDefault = new DeemedSellerRouter(true);
    var merchantDefault = new DeemedSellerRouter(false);

    var p = platformDefault.decide(Map.of(), "acct_x");
    var m = merchantDefault.decide(Map.of(), "acct_x");

    assertThat(p.platformIsDeemedSeller()).isTrue();
    assertThat(m.platformIsDeemedSeller()).isFalse();
  }

  @Test
  void unknownMetadataFallsBackToDefault() {
    var router = new DeemedSellerRouter(true);

    var d = router.decide(Map.of("deemed_seller", "potato"), "acct_x");

    assertThat(d.platformIsDeemedSeller()).isTrue();
  }

  @Test
  void capturesUnderlyingMerchantId() {
    var router = new DeemedSellerRouter(true);

    var d = router.decide(Map.of(), "acct_1234");

    assertThat(d.underlyingMerchantId()).isEqualTo("acct_1234");
  }
}
