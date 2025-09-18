package io.github.mateokadiu.moss.shared;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class IdsTest {

  @Test
  void generatesV7() {
    UUID a = Ids.newV7();
    UUID b = Ids.newV7();
    assertThat(a).isNotEqualTo(b);
    assertThat(a.version()).isEqualTo(7);
    assertThat(b.version()).isEqualTo(7);
  }

  @Test
  void v7IsTimeSortable() throws InterruptedException {
    UUID a = Ids.newV7();
    Thread.sleep(2);
    UUID b = Ids.newV7();
    // v7 sorts lexicographically by time
    assertThat(a.toString().compareTo(b.toString())).isLessThan(0);
  }
}
